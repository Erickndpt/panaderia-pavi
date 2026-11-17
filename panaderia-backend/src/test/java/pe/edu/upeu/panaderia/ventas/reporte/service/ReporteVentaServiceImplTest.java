package pe.edu.upeu.panaderia.ventas.reporte.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import pe.edu.upeu.panaderia.ventas.reporte.dto.ReporteVentaResponse;
import pe.edu.upeu.panaderia.ventas.venta.entity.EstadoVenta;
import pe.edu.upeu.panaderia.ventas.venta.entity.TipoVenta;
import pe.edu.upeu.panaderia.ventas.venta.entity.Venta;
import pe.edu.upeu.panaderia.ventas.venta.repository.VentaRepository;

@ExtendWith(MockitoExtension.class)
class ReporteVentaServiceImplTest {

    @Mock
    private VentaRepository ventaRepository;

    @InjectMocks
    private ReporteVentaServiceImpl reporteVentaService;

    private Venta ventaDe(TipoVenta tipo, String total) {
        Venta venta = new Venta();
        venta.setEstado(EstadoVenta.REGISTRADA);
        venta.setTipo(tipo);
        venta.setTotal(new BigDecimal(total));
        return venta;
    }

    @Test
    @DisplayName("Generar reporte con ventas calcula totales por tipo")
    void generarConVentasDeberiaCalcularTotalesPorTipo() {
        when(ventaRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(
                        ventaDe(TipoVenta.LOCAL, "7.00"),
                        ventaDe(TipoVenta.EXPORTACION, "11.50"),
                        ventaDe(TipoVenta.LOCAL, "3.25")));

        ReporteVentaResponse reporte = reporteVentaService.generar(
                LocalDateTime.of(2026, 9, 1, 0, 0),
                LocalDateTime.of(2026, 9, 30, 23, 59, 59));

        assertThat(reporte.cantidadVentas()).isEqualTo(3);
        assertThat(reporte.montoTotal()).isEqualByComparingTo("21.75");
        assertThat(reporte.cantidadLocales()).isEqualTo(2);
        assertThat(reporte.montoLocal()).isEqualByComparingTo("10.25");
        assertThat(reporte.cantidadExportaciones()).isEqualTo(1);
        assertThat(reporte.montoExportacion()).isEqualByComparingTo("11.50");
    }

    @Test
    @DisplayName("Generar reporte sin ventas devuelve cero en todos los campos")
    void generarSinVentasDeberiaDevolverCeros() {
        when(ventaRepository.findAll(any(Specification.class))).thenReturn(List.of());

        ReporteVentaResponse reporte = reporteVentaService.generar(null, null);

        assertThat(reporte.cantidadVentas()).isZero();
        assertThat(reporte.montoTotal()).isZero();
        assertThat(reporte.cantidadLocales()).isZero();
        assertThat(reporte.montoLocal()).isZero();
        assertThat(reporte.cantidadExportaciones()).isZero();
        assertThat(reporte.montoExportacion()).isZero();
    }

    @Test
    @DisplayName("Generar reporte excluye las ventas ANULADA")
    void generarDeberiaExcluirVentasAnuladas() {
        Venta localRegistrada = ventaDe(TipoVenta.LOCAL, "7.00");
        Venta exportacionAnulada = ventaDe(TipoVenta.EXPORTACION, "50.00");
        exportacionAnulada.setEstado(EstadoVenta.ANULADA);
        Venta localAnulada = ventaDe(TipoVenta.LOCAL, "20.00");
        localAnulada.setEstado(EstadoVenta.ANULADA);
        when(ventaRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(localRegistrada, exportacionAnulada, localAnulada));

        ReporteVentaResponse reporte = reporteVentaService.generar(null, null);

        assertThat(reporte.cantidadVentas()).isEqualTo(1);
        assertThat(reporte.montoTotal()).isEqualByComparingTo("7.00");
        assertThat(reporte.cantidadLocales()).isEqualTo(1);
        assertThat(reporte.montoLocal()).isEqualByComparingTo("7.00");
        assertThat(reporte.cantidadExportaciones()).isZero();
        assertThat(reporte.montoExportacion()).isZero();
    }
}