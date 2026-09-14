-- E4Ad-HU06: la Penalizacion necesitaba tres datos que la spec pide y que no existian.
--
--  * monto: sin el, una penalizacion Economica no dice cuanto se cobra (criterio 5).
--  * fecha_inicio / fecha_fin: sin vigencia, una "Suspension temporal" no tiene fin y nada
--    la puede levantar automaticamente (criterio 4).
--  * denuncia_id: la penalizacion nacida de una denuncia resuelta quedaba desvinculada de
--    su origen, asi que el listado no podia ofrecer el enlace "Ver denuncia" (criterio 6).
--
-- Todas nullable: las penalizaciones ya cargadas no tienen estos datos, y monto/vigencia
-- solo aplican segun el tipo.
ALTER TABLE penalizacion ADD COLUMN monto NUMERIC(10,2);
ALTER TABLE penalizacion ADD COLUMN fecha_inicio DATE;
ALTER TABLE penalizacion ADD COLUMN fecha_fin DATE;
ALTER TABLE penalizacion ADD COLUMN denuncia_id UUID REFERENCES denuncia(id);

CREATE INDEX ix_penalizacion_denuncia ON penalizacion (denuncia_id);
