package org.kie.kogito.openapi.openapi.model;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.fasterxml.jackson.annotation.JsonProperty;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public class A16220Request  {

    private String accountB;
    private Integer AMT;
    private Boolean EC;

    /**
    * Get accountB
    * @return accountB
    **/
    @JsonProperty("Account_B")
    public String getAccountB() {
        return accountB;
    }

    /**
     * Set accountB
     **/
    public void setAccountB(String accountB) {
        this.accountB = accountB;
    }

    public A16220Request accountB(String accountB) {
        this.accountB = accountB;
        return this;
    }

    /**
    * Get AMT
    * @return AMT
    **/
    @JsonProperty("AMT")
    public Integer getAMT() {
        return AMT;
    }

    /**
     * Set AMT
     **/
    public void setAMT(Integer AMT) {
        this.AMT = AMT;
    }

    public A16220Request AMT(Integer AMT) {
        this.AMT = AMT;
        return this;
    }

    /**
    * Get EC
    * @return EC
    **/
    @JsonProperty("EC")
    public Boolean getEC() {
        return EC;
    }

    /**
     * Set EC
     **/
    public void setEC(Boolean EC) {
        this.EC = EC;
    }

    public A16220Request EC(Boolean EC) {
        this.EC = EC;
        return this;
    }

    /**
     * Create a string representation of this pojo.
     **/
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class A16220Request {\n");

        sb.append("    accountB: ").append(toIndentedString(accountB)).append("\n");
        sb.append("    AMT: ").append(toIndentedString(AMT)).append("\n");
        sb.append("    EC: ").append(toIndentedString(EC)).append("\n");
        
        sb.append("}");
        return sb.toString();
    }

    /**
     * Compares this object to the specified object. The result is
     * {@code true} if and only if the argument is not
     * {@code null} and is a {@code A16220Request} object that
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

        A16220Request model = (A16220Request) obj;

        return java.util.Objects.equals(accountB, model.accountB) &&
        java.util.Objects.equals(AMT, model.AMT) &&
        java.util.Objects.equals(EC, model.EC);
    }

    /**
     * Returns a hash code for a {@code A16220Request}.
     *
     * @return a hash code value for a {@code A16220Request}.
     **/
    @Override
    public int hashCode() {
        return java.util.Objects.hash(accountB,
        AMT,
        EC);
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
    public static class A16220RequestQueryParam  {

        @jakarta.ws.rs.QueryParam("accountB")
        private String accountB;
        @jakarta.ws.rs.QueryParam("AMT")
        private Integer AMT;
        @jakarta.ws.rs.QueryParam("EC")
        private Boolean EC;

        /**
        * Get accountB
        * @return accountB
        **/
        @com.fasterxml.jackson.annotation.JsonProperty("Account_B")
        public String getAccountB() {
            return accountB;
        }

        /**
         * Set accountB
         **/
        public void setAccountB(String accountB) {
            this.accountB = accountB;
        }

        public A16220RequestQueryParam accountB(String accountB) {
            this.accountB = accountB;
            return this;
        }

        /**
        * Get AMT
        * @return AMT
        **/
        @com.fasterxml.jackson.annotation.JsonProperty("AMT")
        public Integer getAMT() {
            return AMT;
        }

        /**
         * Set AMT
         **/
        public void setAMT(Integer AMT) {
            this.AMT = AMT;
        }

        public A16220RequestQueryParam AMT(Integer AMT) {
            this.AMT = AMT;
            return this;
        }

        /**
        * Get EC
        * @return EC
        **/
        @com.fasterxml.jackson.annotation.JsonProperty("EC")
        public Boolean getEC() {
            return EC;
        }

        /**
         * Set EC
         **/
        public void setEC(Boolean EC) {
            this.EC = EC;
        }

        public A16220RequestQueryParam EC(Boolean EC) {
            this.EC = EC;
            return this;
        }

        /**
         * Create a string representation of this pojo.
         **/
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("class A16220RequestQueryParam {\n");

            sb.append("    accountB: ").append(toIndentedString(accountB)).append("\n");
            sb.append("    AMT: ").append(toIndentedString(AMT)).append("\n");
            sb.append("    EC: ").append(toIndentedString(EC)).append("\n");
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
