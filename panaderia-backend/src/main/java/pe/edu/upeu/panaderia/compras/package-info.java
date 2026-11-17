@org.springframework.modulith.ApplicationModule(displayName = "Compras",
        allowedDependencies = {
                "exception",
                "inventario::insumo-service",
                "inventario::insumo-dto",
                "finanzas::finanzas-service"
        })
package pe.edu.upeu.panaderia.compras;