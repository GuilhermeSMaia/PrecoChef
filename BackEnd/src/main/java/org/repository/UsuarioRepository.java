package org.repository;

import org.entity.ListasCompras;
import org.entity.Usuarios;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UsuarioRepository implements PanacheRepositoryBase<Usuarios, Long> {

    public Usuarios findByEmail(String email) {
        if (email == null) return null;
        return find("email", email.trim().toLowerCase()).firstResult();
    }

    public boolean existeAdmin() {
        return count("role", Usuarios.ROLE_ADMIN) > 0;
    }

    public ListasCompras findLista(Usuarios usuario) {
        return ListasCompras.find("usuario", usuario).firstResult();
    }
}
