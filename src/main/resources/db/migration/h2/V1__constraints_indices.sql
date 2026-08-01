ALTER TABLE paciente ADD CONSTRAINT uq_paciente_documento UNIQUE (documento_identidad);

CREATE UNIQUE INDEX ux_cita_medico_fecha
    ON cita (medico_id, fecha_hora);