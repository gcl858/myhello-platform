package org.kie.kogito.openapi.openapi.model;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.fasterxml.jackson.annotation.JsonProperty;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public class HealthResponse  {

    private String status;
    private List<Map<String, Object>> checks;

    /**
    * Get status
    * @return status
    **/
    @JsonProperty("status")
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    public String getStatus() {
        return status;
    }

    /**
     * Set status
     **/
    public void setStatus(String status) {
        this.status = status;
    }

    public HealthResponse status(String status) {
        this.status = status;
        return this;
    }

    /**
    * Get checks
    * @return checks
    **/
    @JsonProperty("checks")
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    public List<Map<String, Object>> getChecks() {
        return checks;
    }

    /**
     * Set checks
     **/
    public void setChecks(List<Map<String, Object>> checks) {
        this.checks = checks;
    }

    public HealthResponse checks(List<Map<String, Object>> checks) {
        this.checks = checks;
        return this;
    }
    public HealthResponse addChecksItem(Map<String, Object> checksItem) {
        if (this.checks == null){
            checks = new ArrayList<>();
        }
        this.checks.add(checksItem);
        return this;
    }

    /**
     * Create a string representation of this pojo.
     **/
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class HealthResponse {\n");

        sb.append("    status: ").append(toIndentedString(status)).append("\n");
        sb.append("    checks: ").append(toIndentedString(checks)).append("\n");
        
        sb.append("}");
        return sb.toString();
    }

    /**
     * Compares this object to the specified object. The result is
     * {@code true} if and only if the argument is not
     * {@code null} and is a {@code HealthResponse} object that
     * contains the same values as this object.
     *
     * @param   obj   the object to compare with.
     * @return  {@code true} if the objects are the same;
     *          {@code false} otherwise.
     **/
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        HealthResponse model = (HealthResponse) obj;

        return java.util.Objects.equals(status, model.status) &&
        java.util.Objects.equals(checks, model.checks);
    }

    /**
     * Returns a hash code for a {@code HealthResponse}.
     *
     * @return a hash code value for a {@code HealthResponse}.
     **/
    @Override
    public int hashCode() {
        return java.util.Objects.hash(status,
        checks);
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private static String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class HealthResponseQueryParam  {

        @jakarta.ws.rs.QueryParam("status")
        private String status;
        @jakarta.ws.rs.QueryParam("checks")
        private List<Map<String, Object>> checks = null;

        /**
        * Get status
        * @return status
        **/
        @com.fasterxml.jackson.annotation.JsonProperty("status")
        public String getStatus() {
            return status;
        }

        /**
         * Set status
         **/
        public void setStatus(String status) {
            this.status = status;
        }

        public HealthResponseQueryParam status(String status) {
            this.status = status;
            return this;
        }

        /**
        * Get checks
        * @return checks
        **/
        @com.fasterxml.jackson.annotation.JsonProperty("checks")
        public List<Map<String, Object>> getChecks() {
            return checks;
        }

        /**
         * Set checks
         **/
        public void setChecks(List<Map<String, Object>> checks) {
            this.checks = checks;
        }

        public HealthResponseQueryParam checks(List<Map<String, Object>> checks) {
            this.checks = checks;
            return this;
        }
        public HealthResponseQueryParam addChecksItem(Map<String, Object> checksItem) {
            this.checks.add(checksItem);
            return this;
        }

        /**
         * Create a string representation of this pojo.
         **/
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("class HealthResponseQueryParam {\n");

            sb.append("    status: ").append(toIndentedString(status)).append("\n");
            sb.append("    checks: ").append(toIndentedString(checks)).append("\n");
            sb.append("}");
            return sb.toString();
        }

        /**
         * Convert the given object to string with each line indented by 4 spaces
         * (except the first line).
         */
        private static String toIndentedString(Object o) {
            if (o == null) {
                return "null";
            }
            return o.toString().replace("\n", "\n    ");
        }
    }}
