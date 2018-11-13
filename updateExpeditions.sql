-- For Migration on 11/13/18

CREATE OR REPLACE FUNCTION expeditions_tsv_trigger()
  RETURNS trigger
LANGUAGE plpgsql
AS $function$
begin
  new.tsv = to_tsvector(string_agg((j).value::text, ' ')) from jsonb_each(new.metadata) as j;
  return new;
end
$function$;

ALTER TABLE expeditions ADD COLUMN tsv TSVECTOR;
CREATE TRIGGER tsvector_update BEFORE INSERT OR UPDATE ON expeditions FOR EACH ROW EXECUTE PROCEDURE expeditions_tsv_trigger();
CREATE INDEX idx_expeditions_tsv ON expeditions USING GIN (tsv);

update expeditions set metadata = metadata;