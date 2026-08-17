package org.kie.kogito.openapi.openapi.model;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.fasterxml.jackson.annotation.JsonProperty;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public class A16229Response  {

    private String code;
    private String message;
    private String account;

    /**
    * Get code
    * @return code
    **/
    @JsonProperty("code")
    public String getCode() {
        return code;
    }

    /**
     * Set code
     **/
    public void setCode(String code) {
        this.code = code;
    }

    public A16229Response code(String code) {
        this.code = code;
        return this;
    }

    /**
    * Get message
    * @return message
    **/
    @JsonProperty("message")
    public String getMessage() {
        return message;
    }

    /**
     * Set message
     **/
    public void setMessage(String message) {
        this.message = message;
    }

    public A16229Response message(String message) {
        this.message = message;
        return this;
    }

    /**
    * Get account
    * @return account
    **/
    @JsonProperty("Account")
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    public String getAccount() {
        return account;
    }

    /**
     * Set account
     **/
    public void setAccount(String account) {
        this.account = account;
    }

    public A16229Response account(String account) {
        this.account = account;
        return this;
    }

    /**
     * Create a string representation of this pojo.
     **/
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class A16229Response {\n");

        sb.append("    code: ").append(toIndentedString(code)).append("\n");
        sb.append("    message: ").append(toIndentedString(message)).append("\n");
        sb.append("    account: ").append(toIndentedString(account)).append("\n");
        
        sb.append("}");
        return sb.toString();
    }

    /**
     * Compares this object to the specified object. The result is
     * {@code true} if and only if the argument is not
     * {@code null} and is a {@code A16229Response} object that
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

        A16229Response model = (A16229Response) obj;

        return java.util.Objects.equals(code, model.code) &&
        java.util.Objects.equals(message, model.message) &&
        java.util.Objects.equals(account, model.account);
    }

    /**
     * Returns a hash code for a {@code A16229Response}.
     *
     * @return a hash code value for a {@code A16229Response}.
     **/
    @Override
    public int hashCode() {
        return java.util.Objects.hash(code,
        message,
        account);
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
    public static class A16229ResponseQueryParam  {

        @jakarta.ws.rs.QueryParam("code")
        private String code;
        @jakarta.ws.rs.QueryParam("message")
        private String message;
        @jakarta.ws.rs.QueryParam("account")
        private String account;

        /**
        * Get code
        * @return code
        **/
        @com.fasterxml.jackson.annotation.JsonProperty("code")
        public String getCode() {
            return code;
        }

        /**
         * Set code
         **/
        public void setCode(String code) {
            this.code = code;
        }

        public A16229ResponseQueryParam code(String code) {
            this.code = code;
            return this;
        }

        /**
        * Get message
        * @return message
        **/
        @com.fasterxml.jackson.annotation.JsonProperty("message")
        public String getMessage() {
            return message;
        }

        /**
         * Set message
         **/
        public void setMessage(String message) {
            this.message = message;
        }

        public A16229ResponseQueryParam message(String message) {
            this.message = message;
            return this;
        }

        /**
        * Get account
        * @return account
        **/
        @com.fasterxml.jackson.annotation.JsonProperty("Account")
        public String getAccount() {
            return account;
        }

        /**
         * Set account
         **/
        public void setAccount(String account) {
            this.account = account;
        }

        public A16229ResponseQueryParam account(String account) {
            this.account = account;
            return this;
        }

        /**
         * Create a string representation of this pojo.
         **/
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("class A16229ResponseQueryParam {\n");

            sb.append("    code: ").append(toIndentedString(code)).append("\n");
            sb.append("    message: ").append(toIndentedString(message)).append("\n");
            sb.append("    account: ").append(toIndentedString(account)).append("\n");
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
