---
-- #%L
-- DeviceHive Dao RDBMS Implementation
-- %%
-- Copyright (C) 2016 DataArt
-- %%
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--      http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
-- #L%
---

ALTER TABLE plugin DROP CONSTRAINT plugin_name_unique;

-- Create Icomponent table
CREATE TABLE icomponent (
  id             BIGSERIAL    NOT NULL,
  name           VARCHAR(128) NOT NULL,
  description    VARCHAR(128) NULL,
  entity_version BIGINT       NOT NULL DEFAULT 0
);

ALTER TABLE icomponent ADD CONSTRAINT icomponent_pk PRIMARY KEY (id);
ALTER TABLE icomponent ADD CONSTRAINT icomponent_name_unique UNIQUE (name);

-- Create User-Icomponent table
CREATE TABLE user_icomponent (
  id             BIGSERIAL NOT NULL,
  user_id        BIGINT    NOT NULL,
  icomponent_id     BIGINT    NOT NULL,
  entity_version BIGINT DEFAULT 0
);

ALTER TABLE user_icomponent ADD CONSTRAINT user_icomponent_pk PRIMARY KEY (id);
ALTER TABLE user_icomponent ADD CONSTRAINT user_icomponent_user_pk FOREIGN KEY (user_id) REFERENCES dh_user (id);
ALTER TABLE user_icomponent ADD CONSTRAINT user_icomponent_icomponent_pk FOREIGN KEY (icomponent_id) REFERENCES icomponent (id);

-- Add icomponent column for device table
ALTER TABLE device ADD COLUMN icomponent_id BIGINT NULL;
ALTER TABLE device ADD CONSTRAINT device_icomponent_fk FOREIGN KEY (icomponent_id) REFERENCES icomponent (id) ON DELETE CASCADE;
CREATE INDEX device_icomponent_id_idx ON device(icomponent_id);

-- Default icomponent
INSERT INTO icomponent (name, description)
  VALUES
  ('Default Icomponent', 'Default DeviceHive icomponent');
INSERT INTO user_icomponent (user_id, icomponent_id) VALUES (1, 1);
UPDATE device SET icomponent_id = 1 WHERE device_id = 'e50d6085-2aba-48e9-b1c3-73c673e414be';

-- Insert some default icomponents
INSERT INTO icomponent (name, description)
  VALUES
  ('Room', 'Room icomponent');

INSERT INTO icomponent (name, description)
  VALUES
  ('Wall', 'Wall icomponent');

INSERT INTO icomponent (name, description)
  VALUES
  ('Floor', 'Floor icomponent');

INSERT INTO icomponent (name, description)
  VALUES
  ('Ceiling', 'Ceiling icomponent');

INSERT INTO icomponent (name, description)
  VALUES
  ('Door', 'Door icomponent');

INSERT INTO icomponent (name, description)
  VALUES
  ('HVAC', 'HVAC icomponent');
  
-- Add allIcomponentsAvailable column to users
ALTER TABLE dh_user ADD COLUMN all_icomponents_available BOOLEAN DEFAULT TRUE;

-- Set allIcomponentsAvailable to true if it is null
UPDATE dh_user SET all_icomponents_available = TRUE WHERE all_icomponents_available IS NULL;