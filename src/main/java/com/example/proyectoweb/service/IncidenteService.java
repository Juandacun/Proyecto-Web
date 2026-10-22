package com.example.proyectoweb.service;

import com.example.proyectoweb.dto.IncidenteDTO;
import com.example.proyectoweb.model.Incidente;
import com.example.proyectoweb.model.Recurso;
import com.example.proyectoweb.repository.IncidenteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class IncidenteService {

    @Autowired
    private IncidenteRepository repository;

    @Autowired
    private RecursoService recursoService;

    @Transactional(readOnly = true)
    public List<Incidente> listar() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Incidente> listarFiltrado(Long recursoId, String severidad) {
        if (recursoId != null && severidad != null && !severidad.isBlank()) {
            return repository.findByRecursoIdRecursoAndSeveridadIgnoreCase(recursoId, severidad.trim());
        }
        if (recursoId != null) {
            return repository.findByRecursoIdRecurso(recursoId);
        }
        if (severidad != null && !severidad.isBlank()) {
            return repository.findBySeveridadIgnoreCase(severidad.trim());
        }
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Incidente buscarPorId(Long id) {
        if (id == null) return null;
        return repository.findById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public Optional<Incidente> buscarPorIdOptional(Long id) {
        if (id == null) return Optional.empty();
        return repository.findById(id);
    }

    // RN-04: Reporte de incidente crítico cambia el estado del recurso a bloqueado
    public Incidente registrarIncidente(IncidenteDTO dto) {
    if (dto == null) {
        throw new IllegalArgumentException("Los datos del incidente no pueden ser nulos.");
    }
    if (dto.getRecursoId() == null) {
        throw new IllegalArgumentException("El ID del recurso afectado es obligatorio.");
    }
    if (dto.getDescripcion() == null || dto.getDescripcion().isBlank()) {
        throw new IllegalArgumentException("La descripción de la novedad o falla es obligatoria.");
    }

    String descripcion = dto.getDescripcion().trim();                        // NUEVO
    if (descripcion.length() > 255) {                                        // NUEVO
        throw new IllegalArgumentException("La descripción no puede superar 255 caracteres.");
    }

    Recurso recurso = recursoService.buscarPorId(dto.getRecursoId());
    if (recurso == null) {
        throw new IllegalArgumentException("El recurso con ID " + dto.getRecursoId() + " no existe.");
    }

    String severidad = normalizarSeveridad(dto.getSeveridad());
    String tipo = normalizarTipo(dto.getTipoNovedad());                    

    Incidente incidente = new Incidente();
    incidente.setRecurso(recurso);
    incidente.setTipo(tipo);
    incidente.setSeveridad(severidad);
    incidente.setDescripcion(descripcion);                                  
    incidente.setFecha(LocalDateTime.now());                               
    incidente.setReportadoPor(dto.getUsuarioId() != null ? dto.getUsuarioId().trim() : "Anónimo");

    Incidente guardado = repository.save(incidente);

    // RN-04: Si la severidad es Crítica, bloquear automáticamente el recurso
    if (esSeveridadCritica(severidad)) {
        recurso.setEstado("Bloqueado");
        recursoService.actualizar(recurso);
    }

    return guardado;
}

    // Sobrecarga para compatibilidad con código existente
    public Incidente guardar(Incidente incidente) {
        if (incidente == null) {
            throw new IllegalArgumentException("El incidente no puede ser nulo.");
        }
        if (incidente.getRecurso() != null && esSeveridadCritica(incidente.getSeveridad())) {
            Recurso recurso = incidente.getRecurso();
            recurso.setEstado("Bloqueado");
            recursoService.actualizar(recurso);
        }
        return repository.save(incidente);
    }

    private boolean esSeveridadCritica(String severidad) {
        if (severidad == null) return false;
        String s = java.text.Normalizer.normalize(severidad, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase();
        return s.contains("critic");
    }

    private static final List<String> TIPOS = List.of(
        "Daño físico", "Falla de funcionamiento",
        "Accesorio faltante", "Condición inadecuada del espacio");

    private String normalizarSeveridad(String severidad) {
        if (severidad == null || severidad.isBlank()) {
            return "Media";
        }
        String s = sinTildes(severidad);
        if (s.contains("critic")) return "Crítica";
        if (s.contains("leve")) return "Leve";
        if (s.contains("media")) return "Media";
        throw new IllegalArgumentException("Severidad no válida. Opciones: Leve, Media, Crítica.");
    }

    private String normalizarTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return "Daño físico";
        }
        String buscado = sinTildes(tipo);
        for (String t : TIPOS) {
            if (sinTildes(t).equals(buscado)) return t;
        }
        throw new IllegalArgumentException(
                "Tipo de novedad no válido. Opciones: " + String.join(", ", TIPOS));
    }

    private String sinTildes(String texto) {
        return java.text.Normalizer.normalize(texto.trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase();
    }
}