CREATE OR REPLACE FUNCTION update_config(config JSONB)
  RETURNS JSONB AS $$
DECLARE
  result     JSONB;
  entities   JSONB;
  lists JSONB;
  newList    JSONB;
  fields JSONB;
  newEntity  JSONB;
  attributes JSONB;
  e          JSONB;
  a          JSONB;
  l          JSONB;
  f          JSONB;
BEGIN
  IF jsonb_typeof(config) = 'null' OR config = '{}' :: JSONB THEN
    RETURN config;
  END IF;

  result = config;
  entities := '[]'::JSONB;
  FOR e IN SELECT * FROM jsonb_array_elements((config->>'entities')::JSONB) LOOP
    newEntity = e;
    attributes := '[]'::JSONB;
    FOR a IN SELECT * FROM jsonb_array_elements((e->>'attributes')::JSONB) LOOP
      IF a ? 'datatype' THEN
        a = a || jsonb_build_object('dataType', a->>'datatype');
        a = a - 'datatype';
      END IF;
      IF a ? 'defined_by' THEN
        a = a || jsonb_build_object('definedBy', a->>'defined_by');
        a = a - 'defined_by';
      END IF;
      IF a ? 'delimited_by' THEN
        a = a || jsonb_build_object('delimitedBy', a->>'delimited_by');
        a = a - 'delimited_by';
      END IF;
      IF a ? 'dataformat' THEN
        a = a || jsonb_build_object('dataFormat', a->>'dataformat');
        a = a - 'dataformat';
      END IF;
      a = a - 'displayAnnotationProperty';

      attributes = attributes || jsonb_build_array(a);
    END LOOP;
    newEntity = newEntity || jsonb_build_object('attributes', attributes);
    entities = entities || jsonb_build_array(newEntity);
  END LOOP;
  result = result || jsonb_build_object('entities', entities);

  lists := '[]'::JSONB;
  FOR l IN SELECT * FROM jsonb_array_elements((config->>'lists')::JSONB) LOOP
    fields := '[]'::JSONB;
    FOR f IN SELECT * FROM jsonb_array_elements((l->>'fields')::JSONB) LOOP
      IF f ? 'defined_by' THEN
        f = f || jsonb_build_object('definedBy', f->>'defined_by');
        f = f - 'defined_by';
      END IF;

      fields = fields || jsonb_build_array(f);
    END LOOP;
    newList = l || jsonb_build_object('fields', fields);
    lists = lists || jsonb_build_array(newList);
  END LOOP;
  result = result || jsonb_build_object('lists', lists);


  RETURN result;

END;
$$ LANGUAGE plpgsql;

update projects set config = update_config(config);

DROP FUNCTION update_config(JSONB);
