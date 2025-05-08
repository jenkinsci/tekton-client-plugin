package io.kubernetes.client.openapi.models;

import java.lang.SuppressWarnings;
import io.kubernetes.client.fluent.BaseFluent;
import java.lang.Object;
import java.lang.String;

/**
 * Generated
 */
@SuppressWarnings("unchecked")
public class V1ResourceClaimFluent<A extends V1ResourceClaimFluent<A>> extends BaseFluent<A>{
  public V1ResourceClaimFluent() {
  }
  
  public V1ResourceClaimFluent(V1ResourceClaim instance) {
    this.copyInstance(instance);
  }
  private String name;
  private String request;
  
  protected void copyInstance(V1ResourceClaim instance) {
    instance = (instance != null ? instance : new V1ResourceClaim());
    if (instance != null) {
          this.withName(instance.getName());
          this.withRequest(instance.getRequest());
        }
  }
  
  public String getName() {
    return this.name;
  }
  
  public A withName(String name) {
    this.name = name;
    return (A) this;
  }
  
  public boolean hasName() {
    return this.name != null;
  }
  
  public String getRequest() {
    return this.request;
  }
  
  public A withRequest(String request) {
    this.request = request;
    return (A) this;
  }
  
  public boolean hasRequest() {
    return this.request != null;
  }
  
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    V1ResourceClaimFluent that = (V1ResourceClaimFluent) o;
    if (!java.util.Objects.equals(name, that.name)) return false;
    if (!java.util.Objects.equals(request, that.request)) return false;
    return true;
  }
  
  public int hashCode() {
    return java.util.Objects.hash(name,  request,  super.hashCode());
  }
  
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    if (name != null) { sb.append("name:"); sb.append(name + ","); }
    if (request != null) { sb.append("request:"); sb.append(request); }
    sb.append("}");
    return sb.toString();
  }
  

}