package pe.edu.upeu.panaderia.finanzas.movimiento.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upeu.panaderia.exception.MovimientoDuplicadoException;
import pe.edu.upeu.panaderia.exception.MovimientoInvalidoException;
import pe.edu.upeu.panaderia.exception.ResourceNotFoundException;
import pe.edu.upeu.panaderia.finanzas.movimiento.dto.MovimientoFinancieroResponse;
import pe.edu.upeu.panaderia.finanzas.movimiento.dto.ResumenFinancieroResponse;
import pe.edu.upeu.panaderia.finanzas.movimiento.entity.EstadoMovimiento;
import pe.edu.upeu.panaderia.finanzas.movimiento.entity.MovimientoFinanciero;
import pe.edu.upeu.panaderia.finanzas.movimiento.entity.OrigenMovimiento;
import pe.edu.upeu.panaderia.finanzas.movimiento.entity.TipoMovimiento;
import pe.edu.upeu.panaderia.finanzas.movimiento.mapper.MovimientoFinancieroMapper;
import pe.edu.upeu.panaderia.finanzas.movimiento.repository.MovimientoFinancieroRepository;

@ExtendWith(MockitoExtension.class)
class FinanzasServiceImplTest {

    @Mock
    private MovimientoFinancieroRepository movimientoRepository;

    @Mock
    private MovimientoFinancieroMapper movimientoMapper;

    @InjectMocks
    private FinanzasServiceImpl finanzasService;

    private LocalDateTime fecha() {
        return LocalDateTime.of(2026, 9, 14, 10, 0);
    }

    private MovimientoFinanciero movimiento(OrigenMovimiento origen, Long origenId, EstadoMovimiento estado) {
        MovimientoFinanciero movimiento = new MovimientoFinanciero();
        movimiento.setId(1L);
        movimiento.setFecha(fecha());
        movimiento.setTipo(origen == OrigenMovimiento.VENTA ? TipoMovimiento.INGRESO : TipoMovimiento.EGRESO);
        movimiento.setOrigen(origen);
        movimiento.setOrigenId(origenId);
        movimiento.setMonto(new BigDecimal("25.00"));
        movimiento.setEstado(estado);
        return movimiento;
    }

    private MovimientoFinancieroResponse respuestaDe(Long id) {
        return new MovimientoFinancieroResponse(
                id,
                fecha(),
                TipoMovimiento.INGRESO,
                OrigenMovimiento.VENTA,
                1L,
                "Ingreso por venta 1 (LOCAL)",
                new BigDecimal("25.00"),
                EstadoMovimiento.ACTIVO);
    }

    @Test
    @DisplayName("Registrar ingreso por venta guarda un movimiento INGRESO/VENTA activo")
    void registrarIngresoVentaDeberiaGuardarIngresoActivo() {
        when(movimientoRepository.findByOrigenAndOrigenId(OrigenMovimiento.VENTA, 1L))
                .thenReturn(Optional.empty());
        when(movimientoRepository.save(any(MovimientoFinanciero.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        finanzasService.registrarIngresoVenta(1L, fecha(), "LOCAL", new BigDecimal("25.00"));

        ArgumentCaptor<MovimientoFinanciero> captor = ArgumentCaptor.forClass(MovimientoFinanciero.class);
        verify(movimientoRepository).save(captor.capture());
        MovimientoFinanciero guardado = captor.getValue();
        assertThat(guardado.getTipo()).isEqualTo(TipoMovimiento.INGRESO);
        assertThat(guardado.getOrigen()).isEqualTo(OrigenMovimiento.VENTA);
        assertThat(guardado.getOrigenId()).isEqualTo(1L);
        assertThat(guardado.getEstado()).isEqualTo(EstadoMovimiento.ACTIVO);
        assertThat(guardado.getFecha()).isEqualTo(fecha());
        assertThat(guardado.getConcepto()).isEqualTo("Ingreso por venta 1 (LOCAL)");
        assertThat(guardado.getMonto()).isEqualByComparingTo(new BigDecimal("25.00"));
    }

    @Test
    @DisplayName("Registrar egreso por compra guarda un movimiento EGRESO/COMPRA activo")
    void registrarEgresoCompraDeberiaGuardarEgresoActivo() {
        when(movimientoRepository.findByOrigenAndOrigenId(OrigenMovimiento.COMPRA, 1L))
                .thenReturn(Optional.empty());
        when(movimientoRepository.save(any(MovimientoFinanciero.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        finanzasService.registrarEgresoCompra(1L, fecha(), new BigDecimal("17.00"));

        ArgumentCaptor<MovimientoFinanciero> captor = ArgumentCaptor.forClass(MovimientoFinanciero.class);
        verify(movimientoRepository).save(captor.capture());
        MovimientoFinanciero guardado = captor.getValue();
        assertThat(guardado.getTipo()).isEqualTo(TipoMovimiento.EGRESO);
        assertThat(guardado.getOrigen()).isEqualTo(OrigenMovimiento.COMPRA);
        assertThat(guardado.getOrigenId()).isEqualTo(1L);
        assertThat(guardado.getEstado()).isEqualTo(EstadoMovimiento.ACTIVO);
        assertThat(guardado.getConcepto()).isEqualTo("Egreso por compra 1");
        assertThat(guardado.getMonto()).isEqualByComparingTo(new BigDecimal("17.00"));
    }

    @Test
    @DisplayName("Anular el ingreso de una venta marca el movimiento como ANULADO sin eliminarlo")
    void anularIngresoVentaExistenteDeberiaMarcarAnulado() {
        MovimientoFinanciero movimiento = movimiento(OrigenMovimiento.VENTA, 1L, EstadoMovimiento.ACTIVO);
        when(movimientoRepository.findByOrigenAndOrigenId(OrigenMovimiento.VENTA, 1L))
                .thenReturn(Optional.of(movimiento));

        finanzasService.anularIngresoVenta(1L);

        assertThat(movimiento.getEstado()).isEqualTo(EstadoMovimiento.ANULADO);
        verify(movimientoRepository, never()).save(any(MovimientoFinanciero.class));
    }

    @Test
    @DisplayName("Registrar un segundo ingreso para la misma venta lanza MovimientoDuplicadoException")
    void registrarIngresoVentaDuplicadaDeberiaLanzarSinGuardar() {
        MovimientoFinanciero movimiento = movimiento(OrigenMovimiento.VENTA, 1L, EstadoMovimiento.ACTIVO);
        when(movimientoRepository.findByOrigenAndOrigenId(OrigenMovimiento.VENTA, 1L))
                .thenReturn(Optional.of(movimiento));

        assertThatThrownBy(() -> finanzasService.registrarIngresoVenta(
                1L, fecha(), "LOCAL", new BigDecimal("25.00")))
                .isInstanceOf(MovimientoDuplicadoException.class)
                .hasMessageContaining("venta")
                .hasMessageContaining("1");

        verify(movimientoRepository, never()).save(any(MovimientoFinanciero.class));
    }

    @Test
    @DisplayName("Registrar un segundo egreso para la misma compra lanza MovimientoDuplicadoException")
    void registrarEgresoCompraDuplicadoDeberiaLanzarSinGuardar() {
        MovimientoFinanciero movimiento = movimiento(OrigenMovimiento.COMPRA, 1L, EstadoMovimiento.ACTIVO);
        when(movimientoRepository.findByOrigenAndOrigenId(OrigenMovimiento.COMPRA, 1L))
                .thenReturn(Optional.of(movimiento));

        assertThatThrownBy(() -> finanzasService.registrarEgresoCompra(
                1L, fecha(), new BigDecimal("17.00")))
                .isInstanceOf(MovimientoDuplicadoException.class)
                .hasMessageContaining("compra")
                .hasMessageContaining("1");

        verify(movimientoRepository, never()).save(any(MovimientoFinanciero.class));
    }

    @Test
    @DisplayName("Registrar un movimiento con monto negativo lanza MovimientoInvalidoException")
    void registrarConMontoNegativoDeberiaLanzar() {
        when(movimientoRepository.findByOrigenAndOrigenId(OrigenMovimiento.VENTA, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> finanzasService.registrarIngresoVenta(
                1L, fecha(), "LOCAL", new BigDecimal("-1")))
                .isInstanceOf(MovimientoInvalidoException.class)
                .hasMessageContaining("mayor o igual a cero");

        verify(movimientoRepository, never()).save(any(MovimientoFinanciero.class));
    }

    @Test
    @DisplayName("Anular el ingreso de una venta inexistente lanza ResourceNotFoundException")
    void anularIngresoVentaInexistenteDeberiaLanzar() {
        when(movimientoRepository.findByOrigenAndOrigenId(OrigenMovimiento.VENTA, 99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> finanzasService.anularIngresoVenta(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Anular el ingreso de una venta ya anulada lanza MovimientoInvalidoException")
    void anularIngresoVentaYaAnuladoDeberiaLanzar() {
        MovimientoFinanciero movimiento = movimiento(OrigenMovimiento.VENTA, 1L, EstadoMovimiento.ANULADO);
        when(movimientoRepository.findByOrigenAndOrigenId(OrigenMovimiento.VENTA, 1L))
                .thenReturn(Optional.of(movimiento));

        assertThatThrownBy(() -> finanzasService.anularIngresoVenta(1L))
                .isInstanceOf(MovimientoInvalidoException.class)
                .hasMessageContaining("ya está anulado");
    }

    @Test
    @DisplayName("Anular el egreso de una compra marca el movimiento como ANULADO sin eliminarlo")
    void anularEgresoCompraExistenteDeberiaMarcarAnulado() {
        MovimientoFinanciero movimiento = movimiento(OrigenMovimiento.COMPRA, 1L, EstadoMovimiento.ACTIVO);
        when(movimientoRepository.findByOrigenAndOrigenId(OrigenMovimiento.COMPRA, 1L))
                .thenReturn(Optional.of(movimiento));

        finanzasService.anularEgresoCompra(1L);

        assertThat(movimiento.getEstado()).isEqualTo(EstadoMovimiento.ANULADO);
        verify(movimientoRepository, never()).save(any(MovimientoFinanciero.class));
    }

    @Test
    @DisplayName("Anular el egreso de una compra inexistente lanza ResourceNotFoundException")
    void anularEgresoCompraInexistenteDeberiaLanzar() {
        when(movimientoRepository.findByOrigenAndOrigenId(OrigenMovimiento.COMPRA, 99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> finanzasService.anularEgresoCompra(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Anular el egreso de una compra ya anulada lanza MovimientoInvalidoException")
    void anularEgresoCompraYaAnuladoDeberiaLanzar() {
        MovimientoFinanciero movimiento = movimiento(OrigenMovimiento.COMPRA, 1L, EstadoMovimiento.ANULADO);
        when(movimientoRepository.findByOrigenAndOrigenId(OrigenMovimiento.COMPRA, 1L))
                .thenReturn(Optional.of(movimiento));

        assertThatThrownBy(() -> finanzasService.anularEgresoCompra(1L))
                .isInstanceOf(MovimientoInvalidoException.class)
                .hasMessageContaining("ya está anulado");
    }

    @Test
    @DisplayName("Listar movimientos financieros devuelve todos los registrados")
    void listarDeberiaDevolverTodos() {
        MovimientoFinanciero primero = movimiento(OrigenMovimiento.VENTA, 1L, EstadoMovimiento.ACTIVO);
        MovimientoFinanciero segundo = movimiento(OrigenMovimiento.COMPRA, 1L, EstadoMovimiento.ACTIVO);
        when(movimientoRepository.findAll()).thenReturn(List.of(primero, segundo));
        when(movimientoMapper.toResponse(primero)).thenReturn(respuestaDe(1L));
        when(movimientoMapper.toResponse(segundo)).thenReturn(respuestaDe(2L));

        List<MovimientoFinancieroResponse> respuestas = finanzasService.listar();

        assertThat(respuestas).hasSize(2);
        assertThat(respuestas).extracting(MovimientoFinancieroResponse::id).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("Obtener un movimiento existente devuelve su respuesta")
    void obtenerExistenteDeberiaDevolverMovimiento() {
        MovimientoFinanciero movimiento = movimiento(OrigenMovimiento.VENTA, 1L, EstadoMovimiento.ACTIVO);
        when(movimientoRepository.findById(1L)).thenReturn(Optional.of(movimiento));
        when(movimientoMapper.toResponse(movimiento)).thenReturn(respuestaDe(1L));

        MovimientoFinancieroResponse respuesta = finanzasService.obtener(1L);

        assertThat(respuesta.id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Obtener un movimiento inexistente lanza ResourceNotFoundException")
    void obtenerInexistenteDeberiaLanzar() {
        when(movimientoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> finanzasService.obtener(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("El resumen suma solo movimientos ACTIVO y calcula el saldo")
    void resumenDeberiaSumarSoloActivos() {
        when(movimientoRepository.contarPorTipoYEstado(TipoMovimiento.INGRESO, EstadoMovimiento.ACTIVO))
                .thenReturn(2L);
        when(movimientoRepository.sumarMontoPorTipoYEstado(TipoMovimiento.INGRESO, EstadoMovimiento.ACTIVO))
                .thenReturn(new BigDecimal("60.00"));
        when(movimientoRepository.contarPorTipoYEstado(TipoMovimiento.EGRESO, EstadoMovimiento.ACTIVO))
                .thenReturn(1L);
        when(movimientoRepository.sumarMontoPorTipoYEstado(TipoMovimiento.EGRESO, EstadoMovimiento.ACTIVO))
                .thenReturn(new BigDecimal("17.00"));

        ResumenFinancieroResponse resumen = finanzasService.resumen();

        assertThat(resumen.cantidadIngresos()).isEqualTo(2L);
        assertThat(resumen.montoIngresos()).isEqualByComparingTo(new BigDecimal("60.00"));
        assertThat(resumen.cantidadEgresos()).isEqualTo(1L);
        assertThat(resumen.montoEgresos()).isEqualByComparingTo(new BigDecimal("17.00"));
        assertThat(resumen.saldo()).isEqualByComparingTo(new BigDecimal("43.00"));
        verify(movimientoRepository, never())
                .contarPorTipoYEstado(TipoMovimiento.INGRESO, EstadoMovimiento.ANULADO);
        verify(movimientoRepository, never())
                .sumarMontoPorTipoYEstado(TipoMovimiento.EGRESO, EstadoMovimiento.ANULADO);
    }
}