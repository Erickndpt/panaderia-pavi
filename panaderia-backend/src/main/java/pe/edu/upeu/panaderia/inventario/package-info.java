@org.springframework.modulith.ApplicationModule(displayName = "Inventario",
        allowedDependencies = {
                "exception",
                "administracion::producto-service",
                "administracion::producto-dto"
        })
package pe.edu.upeu.panaderia.inventario;
