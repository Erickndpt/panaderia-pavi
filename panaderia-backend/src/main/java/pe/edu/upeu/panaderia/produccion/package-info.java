@org.springframework.modulith.ApplicationModule(displayName = "Produccion",
        allowedDependencies = {
                "exception",
                "administracion::producto-service",
                "administracion::producto-dto",
                "inventario::insumo-service",
                "inventario::insumo-dto",
                "inventario::existencia-service"
        })
package pe.edu.upeu.panaderia.produccion;