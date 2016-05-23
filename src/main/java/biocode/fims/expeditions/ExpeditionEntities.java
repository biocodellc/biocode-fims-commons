package biocode.fims.expeditions;

import biocode.fims.bcid.Bcid;
import biocode.fims.bcid.BcidMinter;
import biocode.fims.bcid.ExpeditionMinter;
import biocode.fims.digester.Entity;
import biocode.fims.settings.FimsPrinter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A class to manage expedition entities, storing metadata about the links and information regarding concepts and roots for
 * each expedition entity specified.  Expedition Entities are not meant to be directly associated with any particular semantic web
 * technology. The expedition entities are defined in the project configuration files.
 */
    public class ExpeditionEntities {
        private HashMap<String, String> entities = new HashMap<String, String>();
        private String shortName;
        private String description;
        private String guid;
        private String date;
        private Integer projectId;
        private  String expeditionCode;

        public ExpeditionEntities(Integer projectId, String expeditionCode) {
            this.projectId = projectId;
            this.expeditionCode = expeditionCode;
        }

        /**
         * stores the links between the concept (as URI) and identifier (as String)
         *
         * @return
         */
        public HashMap<String, String> getEntities() {
                                                           return entities;
                                                                           }

        /**
         * sets the links between the concept (as URI) and identifier (as String)
         *
         * @param entities
         */
        public void setEntities(HashMap<String, String> entities) {
                                                                        this.entities = entities;
                                                                                                 }

        /**
         * gets the short name describing this file
         *
         * @return
         */
        public String getShortName() {
                                           return shortName;
                                                            }

        /**
         * sets the short name describing this file
         *
         * @param shortName
         */
        public void setShortName(String shortName) {
                                                         this.shortName = shortName;
                                                                                    }

        /**
         * gets the description for this file
         *
         * @return
         */
        public String getDescription() {
                                             return description;
                                                                }

        /**
         * sets the description for this file
         *
         * @return
         */
        public void setDescription(String description) {
                                                             this.description = description;
                                                                                            }

        public String getGuid() {
                                      return guid;
                                                  }

        public void setGuid(String guid) {
                                               this.guid = guid;
                                                                }

        public String getDate() {
                                      return date;
                                                  }

        public void setDate(String date) {
                                               this.date = date;
                                                                }

        /**
         * Converts this object to a string representation for easy viewing
         *
         * @return
         */
        public String toString() {
                                     StringBuilder sb = new StringBuilder();

                                     sb.append("/**\n");
                                     sb.append("* name = " + shortName + "\n");
                                     sb.append("* description = " + description + "\n");
                                     sb.append("* guid = " + guid + "\n");
                                     sb.append("* date = " + date + "\n");
                                     sb.append("**/\n");
                                     Iterator it = entities.entrySet().iterator();
                                     while (it.hasNext()) {
                                     Map.Entry pairs = (Map.Entry) it.next();
                                     sb.append(pairs.getValue() + " a " + pairs.getKey() + " .\n");
                                     }
                                     return sb.toString();
                                     }


    }
