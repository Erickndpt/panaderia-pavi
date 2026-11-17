package pe.edu.upeu.panaderia.ventas.venta.specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.upeu.panaderia.ventas.venta.entity.Venta;

class VentaSpecificationsTest {

    @Test
    @DisplayName("vendedorIgual filtra por el claim sub del vendedor")
    void vendedorIgualDeberiaFiltrarPorVendedorId() {
        Root<Venta> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        Path<Object> path = mock(Path.class);
        Predicate predicate = mock(Predicate.class);
        when(root.get("vendedorId")).thenReturn(path);
        when(criteriaBuilder.equal(path, "vendedor-1")).thenReturn(predicate);

        Predicate resultado = VentaSpecifications.vendedorIgual("vendedor-1")
                .toPredicate(root, query, criteriaBuilder);

        assertThat(resultado).isSameAs(predicate);
        verify(root).get("vendedorId");
        verify(criteriaBuilder).equal(path, "vendedor-1");
    }
}
