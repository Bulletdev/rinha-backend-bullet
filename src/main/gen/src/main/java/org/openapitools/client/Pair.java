/*
 * Rinha de Backend 2024/Q1 API
 * API para gerenciamento de transações financeiras e consulta de extratos. Desenvolvida com Java 21 utilizando Project Loom (Virtual Threads) para alta concorrência. 
 *
 * The version of the OpenAPI document: 1.0.0
 * Contact: contato@michaelbullet.com
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


package org.openapitools.client;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2025-03-28T14:17:50.441968300-03:00[America/Sao_Paulo]", comments = "Generator version: 7.7.0")
public class Pair {
    private String name = "";
    private String value = "";

    public Pair (String name, String value) {
        setName(name);
        setValue(value);
    }

    private void setName(String name) {
        if (!isValidString(name)) {
            return;
        }

        this.name = name;
    }

    private void setValue(String value) {
        if (!isValidString(value)) {
            return;
        }

        this.value = value;
    }

    public String getName() {
        return this.name;
    }

    public String getValue() {
        return this.value;
    }

    private boolean isValidString(String arg) {
        if (arg == null) {
            return false;
        }

        return true;
    }
}
