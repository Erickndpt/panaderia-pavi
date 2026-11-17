package pe.edu.upeu.panaderia.ventas.pago.service;

import java.math.BigDecimal;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upeu.panaderia.IdentidadJwt;
import pe.edu.upeu.panaderia.exception.AccesoDenegadoException;
import pe.edu.upeu.panaderia.exception.PagoExcedeSaldoException;
import pe.edu.upeu.panaderia.exception.PagoInvalidoException;
import pe.edu.upeu.panaderia.exception.PagoYaAnuladoException;
import pe.edu.upeu.panaderia.exception.ResourceNotFoundException;
import pe.edu.upeu.panaderia.exception.VentaAnuladaNoAceptaPagosException;
import pe.edu.upeu.panaderia.ventas.pago.dto.PagoVentaRequest;
import pe.edu.upeu.panaderia.ventas.pago.dto.PagoVentaResumenResponse;
import pe.edu.upeu.panaderia.ventas.pago.entity.EstadoPago;
import pe.edu.upeu.panaderia.ventas.pago.entity.EstadoPagoVenta;
import pe.edu.upeu.panaderia.ventas.pago.entity.MetodoPago;
import pe.edu.upeu.panaderia.ventas.pago.entity.PagoVenta;
import pe.edu.upeu.panaderia.ventas.pago.mapper.PagoVentaMapper;
import pe.edu.upeu.panaderia.ventas.pago.repository.PagoVentaRepository;
import pe.edu.upeu.panaderia.ventas.AutorizacionVenta;
import pe.edu.upeu.panaderia.ventas.venta.entity.EstadoVenta;
import pe.edu.upeu.panaderia.ventas.venta.entity.Venta;
import pe.edu.upeu.panaderia.ventas.venta.repository.VentaRepository;

@ExtendWith(MockitoExtension.class)
class PagoVentaServiceImplTest {

    @Mock
    private PagoVentaRepository pagoVentaRepository;

    @Mock
    private VentaRepository ventaRepository;

    @Mock
    private PagoVentaMapper pagoVentaMapper;

    @Spy
    private AutorizacionVenta autorizacionVenta;

    @InjectMocks
    private PagoVentaServiceImpl pagoVentaService;

    private IdentidadJwt vendedor(String id) {
        return new IdentidadJwt(id, id, "Vendedor " + id, true);
    }

    private IdentidadJwt supervisor() {
        return new IdentidadJwt("supervisor-1", "supervisor", "Supervisor Uno", false);
    }

    private Venta venta(String total, EstadoVenta estado) {
        Venta venta = new Venta();
        venta.setId(1L);
        venta.setTotal(new BigDecimal(total));
        venta.setEstado(estado);
        return venta;
    }

    private PagoVenta pago(Long id, EstadoPago estado) {
        PagoVenta pago = new PagoVenta();
        pago.setId(id);
        pago.setEstado(estado);
        return pago;
    }

    @Test
    @DisplayName("Registrar pago bloquea la venta, guarda fecha y estado del servidor y normaliza la referencia")
    void registrarDeberiaBloquearVentaYUsarFechaYEstadoDelServidor() {
        Venta venta = venta("100.00", EstadoVenta.REGISTRADA);
        when(ventaRepository.buscarConBloqueoPorId(1L)).thenReturn(Optional.of(venta));
        when(pagoVentaRepository.sumarMontosPorVentaYEstado(1L, EstadoPago.REGISTRADO))
                .thenReturn(new BigDecimal("30.00"));
        when(pagoVentaRepository.save(any(PagoVenta.class))).thenAnswer(inv -> {
            PagoVenta guardado = inv.getArgument(0);
            guardado.setId(50L);
            return guardado;
        });

        pagoVentaService.registrar(1L,
                new PagoVentaRequest(MetodoPago.YAPE, new BigDecimal("20.00"), "  OP-9 "), supervisor());

        verify(ventaRepository).buscarConBloqueoPorId(1L);
        ArgumentCaptor<PagoVenta> captor = ArgumentCaptor.forClass(PagoVenta.class);
        verify(pagoVentaRepository).save(captor.capture());
        PagoVenta guardado = captor.getValue();
        assertThat(guardado.getEstado()).isEqualTo(EstadoPago.REGISTRADO);
        assertThat(guardado.getFecha()).isNotNull();
        assertThat(guardado.getReferencia()).isEqualTo("OP-9");
        assertThat(guardado.getVenta()).isSameAs(venta);
    }

    @Test
    @DisplayName("Una referencia vacía se guarda como NULL")
    void registrarConReferenciaVaciaDeberiaGuardarNull() {
        Venta venta = venta("100.00", EstadoVenta.REGISTRADA);
        when(ventaRepository.buscarConBloqueoPorId(1L)).thenReturn(Optional.of(venta));
        when(pagoVentaRepository.sumarMontosPorVentaYEstado(1L, EstadoPago.REGISTRADO))
                .thenReturn(BigDecimal.ZERO);
        when(pagoVentaRepository.save(any(PagoVenta.class))).thenAnswer(inv -> inv.getArgument(0));

        pagoVentaService.registrar(1L,
                new PagoVentaRequest(MetodoPago.EFECTIVO, new BigDecimal("10.00"), "   "), supervisor());

        ArgumentCaptor<PagoVenta> captor = ArgumentCaptor.forClass(PagoVenta.class);
        verify(pagoVentaRepository).save(captor.capture());
        assertThat(captor.getValue().getReferencia()).isNull();
    }

    @Test
    @DisplayName("Un pago que supera el saldo pendiente lanza PagoExcedeSaldoException y no guarda")
    void registrarExcediendoSaldoDeberiaLanzarYNoGuardar() {
        Venta venta = venta("100.00", EstadoVenta.REGISTRADA);
        when(ventaRepository.buscarConBloqueoPorId(1L)).thenReturn(Optional.of(venta));
        when(pagoVentaRepository.sumarMontosPorVentaYEstado(1L, EstadoPago.REGISTRADO))
                .thenReturn(new BigDecimal("80.00"));

        assertThatThrownBy(() -> pagoVentaService.registrar(
                1L, new PagoVentaRequest(MetodoPago.TARJETA, new BigDecimal("25.00"), null), supervisor()))
                .isInstanceOf(PagoExcedeSaldoException.class)
                .hasMessageContaining("saldo pendiente");

        verify(pagoVentaRepository, never()).save(any(PagoVenta.class));
    }

    @Test
    @DisplayName("Pagar exactamente el saldo pendiente se acepta")
    void registrarExactoAlSaldoDeberiaAceptarse() {
        Venta venta = venta("100.00", EstadoVenta.REGISTRADA);
        when(ventaRepository.buscarConBloqueoPorId(1L)).thenReturn(Optional.of(venta));
        when(pagoVentaRepository.sumarMontosPorVentaYEstado(1L, EstadoPago.REGISTRADO))
                .thenReturn(new BigDecimal("80.00"));
        when(pagoVentaRepository.save(any(PagoVenta.class))).thenAnswer(inv -> inv.getArgument(0));

        pagoVentaService.registrar(1L,
                new PagoVentaRequest(MetodoPago.PLIN, new BigDecimal("20.00"), null), supervisor());

        verify(pagoVentaRepository).save(any(PagoVenta.class));
    }

    @Test
    @DisplayName("Pagar una venta ANULADA lanza VentaAnuladaNoAceptaPagosException")
    void registrarVentaAnuladaDeberiaLanzar() {
        when(ventaRepository.buscarConBloqueoPorId(1L))
                .thenReturn(Optional.of(venta("100.00", EstadoVenta.ANULADA)));

        assertThatThrownBy(() -> pagoVentaService.registrar(
                1L, new PagoVentaRequest(MetodoPago.EFECTIVO, new BigDecimal("10.00"), null), supervisor()))
                .isInstanceOf(VentaAnuladaNoAceptaPagosException.class);

        verify(pagoVentaRepository, never()).save(any(PagoVenta.class));
    }

    @Test
    @DisplayName("Un monto no positivo lanza PagoInvalidoException")
    void registrarMontoNoPositivoDeberiaLanzar() {
        when(ventaRepository.buscarConBloqueoPorId(1L))
                .thenReturn(Optional.of(venta("100.00", EstadoVenta.REGISTRADA)));

        assertThatThrownBy(() -> pagoVentaService.registrar(
                1L, new PagoVentaRequest(MetodoPago.EFECTIVO, BigDecimal.ZERO, null), supervisor()))
                .isInstanceOf(PagoInvalidoException.class);

        verify(pagoVentaRepository, never()).save(any(PagoVenta.class));
    }

    @Test
    @DisplayName("Registrar sobre una venta inexistente propaga ResourceNotFoundException")
    void registrarVentaInexistenteDeberiaLanzar() {
        when(ventaRepository.buscarConBloqueoPorId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pagoVentaService.registrar(
                99L, new PagoVentaRequest(MetodoPago.EFECTIVO, new BigDecimal("10.00"), null), supervisor()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Anular cambia el estado a ANULADO y no borra el pago")
    void anularDeberiaCambiarEstadoYNoBorrar() {
        PagoVenta existente = pago(50L, EstadoPago.REGISTRADO);
        when(pagoVentaRepository.findById(50L)).thenReturn(Optional.of(existente));

        pagoVentaService.anular(50L, supervisor());

        assertThat(existente.getEstado()).isEqualTo(EstadoPago.ANULADO);
        verify(pagoVentaRepository, never()).delete(any(PagoVenta.class));
    }

    @Test
    @DisplayName("Anular dos veces el mismo pago lanza PagoYaAnuladoException")
    void anularDosVecesDeberiaLanzar() {
        when(pagoVentaRepository.findById(50L)).thenReturn(Optional.of(pago(50L, EstadoPago.ANULADO)));

        assertThatThrownBy(() -> pagoVentaService.anular(50L, supervisor()))
                .isInstanceOf(PagoYaAnuladoException.class);
    }

    @Test
    @DisplayName("Resumen sin pagos devuelve PENDIENTE")
    void resumenSinPagosDeberiaSerPendiente() {
        when(ventaRepository.findById(1L)).thenReturn(Optional.of(venta("100.00", EstadoVenta.REGISTRADA)));
        when(pagoVentaRepository.sumarMontosPorVentaYEstado(1L, EstadoPago.REGISTRADO))
                .thenReturn(BigDecimal.ZERO);

        PagoVentaResumenResponse resumen = pagoVentaService.resumen(1L, supervisor());

        assertThat(resumen.estadoPago()).isEqualTo(EstadoPagoVenta.PENDIENTE);
        assertThat(resumen.totalPagado()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resumen.saldoPendiente()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("Resumen con pago parcial devuelve PARCIAL")
    void resumenParcialDeberiaSerParcial() {
        when(ventaRepository.findById(1L)).thenReturn(Optional.of(venta("100.00", EstadoVenta.REGISTRADA)));
        when(pagoVentaRepository.sumarMontosPorVentaYEstado(1L, EstadoPago.REGISTRADO))
                .thenReturn(new BigDecimal("40.00"));

        PagoVentaResumenResponse resumen = pagoVentaService.resumen(1L, supervisor());

        assertThat(resumen.estadoPago()).isEqualTo(EstadoPagoVenta.PARCIAL);
        assertThat(resumen.saldoPendiente()).isEqualByComparingTo(new BigDecimal("60.00"));
    }

    @Test
    @DisplayName("Resumen con el total cubierto devuelve PAGADO")
    void resumenTotalmentePagadoDeberiaSerPagado() {
        when(ventaRepository.findById(1L)).thenReturn(Optional.of(venta("100.00", EstadoVenta.REGISTRADA)));
        when(pagoVentaRepository.sumarMontosPorVentaYEstado(1L, EstadoPago.REGISTRADO))
                .thenReturn(new BigDecimal("100.00"));

        PagoVentaResumenResponse resumen = pagoVentaService.resumen(1L, supervisor());

        assertThat(resumen.estadoPago()).isEqualTo(EstadoPagoVenta.PAGADO);
        assertThat(resumen.saldoPendiente()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Un vendedor no puede registrar pagos sobre una venta ajena")
    void registrarPagoDeVentaAjenaDeberiaLanzarAccesoDenegado() {
        Venta venta = venta("100.00", EstadoVenta.REGISTRADA);
        venta.setVendedorId("vendedor-1");
        when(ventaRepository.buscarConBloqueoPorId(1L)).thenReturn(Optional.of(venta));

        assertThatThrownBy(() -> pagoVentaService.registrar(
                1L, new PagoVentaRequest(MetodoPago.EFECTIVO, new BigDecimal("10.00"), null),
                vendedor("vendedor-2")))
                .isInstanceOf(AccesoDenegadoException.class);

        verify(pagoVentaRepository, never()).save(any(PagoVenta.class));
    }

    @Test
    @DisplayName("Un vendedor no puede consultar pagos de una venta ajena")
    void listarPagosDeVentaAjenaDeberiaLanzarAccesoDenegado() {
        Venta venta = venta("100.00", EstadoVenta.REGISTRADA);
        venta.setVendedorId("vendedor-1");
        when(ventaRepository.findById(1L)).thenReturn(Optional.of(venta));

        assertThatThrownBy(() -> pagoVentaService.listarPorVenta(1L, vendedor("vendedor-2")))
                .isInstanceOf(AccesoDenegadoException.class);

        verify(pagoVentaRepository, never()).findByVentaIdOrderById(any());
    }

    @Test
    @DisplayName("Un vendedor no puede anular pagos")
    void vendedorNoPuedeAnularPago() {
        PagoVenta existente = pago(50L, EstadoPago.REGISTRADO);
        existente.setVenta(venta("100.00", EstadoVenta.REGISTRADA));
        when(pagoVentaRepository.findById(50L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> pagoVentaService.anular(50L, vendedor("vendedor-1")))
                .isInstanceOf(AccesoDenegadoException.class);

        assertThat(existente.getEstado()).isEqualTo(EstadoPago.REGISTRADO);
    }

    @Test
    @DisplayName("tienePagosRegistrados delega en el repositorio con el estado REGISTRADO")
    void tienePagosRegistradosDeberiaDelegar() {
        when(pagoVentaRepository.existePagadoPorVentaYEstado(1L, EstadoPago.REGISTRADO)).thenReturn(true);

        assertThat(pagoVentaService.tienePagosRegistrados(1L)).isTrue();
    }
}