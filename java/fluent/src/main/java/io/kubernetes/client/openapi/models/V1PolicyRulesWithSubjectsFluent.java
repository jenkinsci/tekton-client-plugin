package io.kubernetes.client.openapi.models;

import io.kubernetes.client.fluent.VisitableBuilder;
import java.lang.SuppressWarnings;
import io.kubernetes.client.fluent.Nested;
import java.util.ArrayList;
import java.lang.String;
import java.util.function.Predicate;
import io.kubernetes.client.fluent.BaseFluent;
import java.util.Iterator;
import java.util.List;
import java.util.Collection;
import java.lang.Object;

/**
 * Generated
 */
@SuppressWarnings("unchecked")
public class V1PolicyRulesWithSubjectsFluent<A extends V1PolicyRulesWithSubjectsFluent<A>> extends BaseFluent<A>{
  public V1PolicyRulesWithSubjectsFluent() {
  }
  
  public V1PolicyRulesWithSubjectsFluent(V1PolicyRulesWithSubjects instance) {
    this.copyInstance(instance);
  }
  private ArrayList<V1NonResourcePolicyRuleBuilder> nonResourceRules;
  private ArrayList<V1ResourcePolicyRuleBuilder> resourceRules;
  private ArrayList<FlowcontrolV1SubjectBuilder> subjects;
  
  protected void copyInstance(V1PolicyRulesWithSubjects instance) {
    instance = (instance != null ? instance : new V1PolicyRulesWithSubjects());
    if (instance != null) {
          this.withNonResourceRules(instance.getNonResourceRules());
          this.withResourceRules(instance.getResourceRules());
          this.withSubjects(instance.getSubjects());
        }
  }
  
  public A addToNonResourceRules(int index,V1NonResourcePolicyRule item) {
    if (this.nonResourceRules == null) {this.nonResourceRules = new ArrayList<V1NonResourcePolicyRuleBuilder>();}
    V1NonResourcePolicyRuleBuilder builder = new V1NonResourcePolicyRuleBuilder(item);
    if (index < 0 || index >= nonResourceRules.size()) { _visitables.get("nonResourceRules").add(builder); nonResourceRules.add(builder); } else { _visitables.get("nonResourceRules").add(index, builder); nonResourceRules.add(index, builder);}
    return (A)this;
  }
  
  public A setToNonResourceRules(int index,V1NonResourcePolicyRule item) {
    if (this.nonResourceRules == null) {this.nonResourceRules = new ArrayList<V1NonResourcePolicyRuleBuilder>();}
    V1NonResourcePolicyRuleBuilder builder = new V1NonResourcePolicyRuleBuilder(item);
    if (index < 0 || index >= nonResourceRules.size()) { _visitables.get("nonResourceRules").add(builder); nonResourceRules.add(builder); } else { _visitables.get("nonResourceRules").set(index, builder); nonResourceRules.set(index, builder);}
    return (A)this;
  }
  
  public A addToNonResourceRules(io.kubernetes.client.openapi.models.V1NonResourcePolicyRule... items) {
    if (this.nonResourceRules == null) {this.nonResourceRules = new ArrayList<V1NonResourcePolicyRuleBuilder>();}
    for (V1NonResourcePolicyRule item : items) {V1NonResourcePolicyRuleBuilder builder = new V1NonResourcePolicyRuleBuilder(item);_visitables.get("nonResourceRules").add(builder);this.nonResourceRules.add(builder);} return (A)this;
  }
  
  public A addAllToNonResourceRules(Collection<V1NonResourcePolicyRule> items) {
    if (this.nonResourceRules == null) {this.nonResourceRules = new ArrayList<V1NonResourcePolicyRuleBuilder>();}
    for (V1NonResourcePolicyRule item : items) {V1NonResourcePolicyRuleBuilder builder = new V1NonResourcePolicyRuleBuilder(item);_visitables.get("nonResourceRules").add(builder);this.nonResourceRules.add(builder);} return (A)this;
  }
  
  public A removeFromNonResourceRules(io.kubernetes.client.openapi.models.V1NonResourcePolicyRule... items) {
    if (this.nonResourceRules == null) return (A)this;
    for (V1NonResourcePolicyRule item : items) {V1NonResourcePolicyRuleBuilder builder = new V1NonResourcePolicyRuleBuilder(item);_visitables.get("nonResourceRules").remove(builder); this.nonResourceRules.remove(builder);} return (A)this;
  }
  
  public A removeAllFromNonResourceRules(Collection<V1NonResourcePolicyRule> items) {
    if (this.nonResourceRules == null) return (A)this;
    for (V1NonResourcePolicyRule item : items) {V1NonResourcePolicyRuleBuilder builder = new V1NonResourcePolicyRuleBuilder(item);_visitables.get("nonResourceRules").remove(builder); this.nonResourceRules.remove(builder);} return (A)this;
  }
  
  public A removeMatchingFromNonResourceRules(Predicate<V1NonResourcePolicyRuleBuilder> predicate) {
    if (nonResourceRules == null) return (A) this;
    final Iterator<V1NonResourcePolicyRuleBuilder> each = nonResourceRules.iterator();
    final List visitables = _visitables.get("nonResourceRules");
    while (each.hasNext()) {
      V1NonResourcePolicyRuleBuilder builder = each.next();
      if (predicate.test(builder)) {
        visitables.remove(builder);
        each.remove();
      }
    }
    return (A)this;
  }
  
  public List<V1NonResourcePolicyRule> buildNonResourceRules() {
    return this.nonResourceRules != null ? build(nonResourceRules) : null;
  }
  
  public V1NonResourcePolicyRule buildNonResourceRule(int index) {
    return this.nonResourceRules.get(index).build();
  }
  
  public V1NonResourcePolicyRule buildFirstNonResourceRule() {
    return this.nonResourceRules.get(0).build();
  }
  
  public V1NonResourcePolicyRule buildLastNonResourceRule() {
    return this.nonResourceRules.get(nonResourceRules.size() - 1).build();
  }
  
  public V1NonResourcePolicyRule buildMatchingNonResourceRule(Predicate<V1NonResourcePolicyRuleBuilder> predicate) {
      for (V1NonResourcePolicyRuleBuilder item : nonResourceRules) {
        if (predicate.test(item)) {
          return item.build();
        }
      }
      return null;
  }
  
  public boolean hasMatchingNonResourceRule(Predicate<V1NonResourcePolicyRuleBuilder> predicate) {
      for (V1NonResourcePolicyRuleBuilder item : nonResourceRules) {
        if (predicate.test(item)) {
          return true;
        }
      }
      return false;
  }
  
  public A withNonResourceRules(List<V1NonResourcePolicyRule> nonResourceRules) {
    if (this.nonResourceRules != null) {
      this._visitables.get("nonResourceRules").clear();
    }
    if (nonResourceRules != null) {
        this.nonResourceRules = new ArrayList();
        for (V1NonResourcePolicyRule item : nonResourceRules) {
          this.addToNonResourceRules(item);
        }
    } else {
      this.nonResourceRules = null;
    }
    return (A) this;
  }
  
  public A withNonResourceRules(io.kubernetes.client.openapi.models.V1NonResourcePolicyRule... nonResourceRules) {
    if (this.nonResourceRules != null) {
        this.nonResourceRules.clear();
        _visitables.remove("nonResourceRules");
    }
    if (nonResourceRules != null) {
      for (V1NonResourcePolicyRule item : nonResourceRules) {
        this.addToNonResourceRules(item);
      }
    }
    return (A) this;
  }
  
  public boolean hasNonResourceRules() {
    return this.nonResourceRules != null && !this.nonResourceRules.isEmpty();
  }
  
  public NonResourceRulesNested<A> addNewNonResourceRule() {
    return new NonResourceRulesNested(-1, null);
  }
  
  public NonResourceRulesNested<A> addNewNonResourceRuleLike(V1NonResourcePolicyRule item) {
    return new NonResourceRulesNested(-1, item);
  }
  
  public NonResourceRulesNested<A> setNewNonResourceRuleLike(int index,V1NonResourcePolicyRule item) {
    return new NonResourceRulesNested(index, item);
  }
  
  public NonResourceRulesNested<A> editNonResourceRule(int index) {
    if (nonResourceRules.size() <= index) throw new RuntimeException("Can't edit nonResourceRules. Index exceeds size.");
    return setNewNonResourceRuleLike(index, buildNonResourceRule(index));
  }
  
  public NonResourceRulesNested<A> editFirstNonResourceRule() {
    if (nonResourceRules.size() == 0) throw new RuntimeException("Can't edit first nonResourceRules. The list is empty.");
    return setNewNonResourceRuleLike(0, buildNonResourceRule(0));
  }
  
  public NonResourceRulesNested<A> editLastNonResourceRule() {
    int index = nonResourceRules.size() - 1;
    if (index < 0) throw new RuntimeException("Can't edit last nonResourceRules. The list is empty.");
    return setNewNonResourceRuleLike(index, buildNonResourceRule(index));
  }
  
  public NonResourceRulesNested<A> editMatchingNonResourceRule(Predicate<V1NonResourcePolicyRuleBuilder> predicate) {
    int index = -1;
    for (int i=0;i<nonResourceRules.size();i++) { 
    if (predicate.test(nonResourceRules.get(i))) {index = i; break;}
    } 
    if (index < 0) throw new RuntimeException("Can't edit matching nonResourceRules. No match found.");
    return setNewNonResourceRuleLike(index, buildNonResourceRule(index));
  }
  
  public A addToResourceRules(int index,V1ResourcePolicyRule item) {
    if (this.resourceRules == null) {this.resourceRules = new ArrayList<V1ResourcePolicyRuleBuilder>();}
    V1ResourcePolicyRuleBuilder builder = new V1ResourcePolicyRuleBuilder(item);
    if (index < 0 || index >= resourceRules.size()) { _visitables.get("resourceRules").add(builder); resourceRules.add(builder); } else { _visitables.get("resourceRules").add(index, builder); resourceRules.add(index, builder);}
    return (A)this;
  }
  
  public A setToResourceRules(int index,V1ResourcePolicyRule item) {
    if (this.resourceRules == null) {this.resourceRules = new ArrayList<V1ResourcePolicyRuleBuilder>();}
    V1ResourcePolicyRuleBuilder builder = new V1ResourcePolicyRuleBuilder(item);
    if (index < 0 || index >= resourceRules.size()) { _visitables.get("resourceRules").add(builder); resourceRules.add(builder); } else { _visitables.get("resourceRules").set(index, builder); resourceRules.set(index, builder);}
    return (A)this;
  }
  
  public A addToResourceRules(io.kubernetes.client.openapi.models.V1ResourcePolicyRule... items) {
    if (this.resourceRules == null) {this.resourceRules = new ArrayList<V1ResourcePolicyRuleBuilder>();}
    for (V1ResourcePolicyRule item : items) {V1ResourcePolicyRuleBuilder builder = new V1ResourcePolicyRuleBuilder(item);_visitables.get("resourceRules").add(builder);this.resourceRules.add(builder);} return (A)this;
  }
  
  public A addAllToResourceRules(Collection<V1ResourcePolicyRule> items) {
    if (this.resourceRules == null) {this.resourceRules = new ArrayList<V1ResourcePolicyRuleBuilder>();}
    for (V1ResourcePolicyRule item : items) {V1ResourcePolicyRuleBuilder builder = new V1ResourcePolicyRuleBuilder(item);_visitables.get("resourceRules").add(builder);this.resourceRules.add(builder);} return (A)this;
  }
  
  public A removeFromResourceRules(io.kubernetes.client.openapi.models.V1ResourcePolicyRule... items) {
    if (this.resourceRules == null) return (A)this;
    for (V1ResourcePolicyRule item : items) {V1ResourcePolicyRuleBuilder builder = new V1ResourcePolicyRuleBuilder(item);_visitables.get("resourceRules").remove(builder); this.resourceRules.remove(builder);} return (A)this;
  }
  
  public A removeAllFromResourceRules(Collection<V1ResourcePolicyRule> items) {
    if (this.resourceRules == null) return (A)this;
    for (V1ResourcePolicyRule item : items) {V1ResourcePolicyRuleBuilder builder = new V1ResourcePolicyRuleBuilder(item);_visitables.get("resourceRules").remove(builder); this.resourceRules.remove(builder);} return (A)this;
  }
  
  public A removeMatchingFromResourceRules(Predicate<V1ResourcePolicyRuleBuilder> predicate) {
    if (resourceRules == null) return (A) this;
    final Iterator<V1ResourcePolicyRuleBuilder> each = resourceRules.iterator();
    final List visitables = _visitables.get("resourceRules");
    while (each.hasNext()) {
      V1ResourcePolicyRuleBuilder builder = each.next();
      if (predicate.test(builder)) {
        visitables.remove(builder);
        each.remove();
      }
    }
    return (A)this;
  }
  
  public List<V1ResourcePolicyRule> buildResourceRules() {
    return this.resourceRules != null ? build(resourceRules) : null;
  }
  
  public V1ResourcePolicyRule buildResourceRule(int index) {
    return this.resourceRules.get(index).build();
  }
  
  public V1ResourcePolicyRule buildFirstResourceRule() {
    return this.resourceRules.get(0).build();
  }
  
  public V1ResourcePolicyRule buildLastResourceRule() {
    return this.resourceRules.get(resourceRules.size() - 1).build();
  }
  
  public V1ResourcePolicyRule buildMatchingResourceRule(Predicate<V1ResourcePolicyRuleBuilder> predicate) {
      for (V1ResourcePolicyRuleBuilder item : resourceRules) {
        if (predicate.test(item)) {
          return item.build();
        }
      }
      return null;
  }
  
  public boolean hasMatchingResourceRule(Predicate<V1ResourcePolicyRuleBuilder> predicate) {
      for (V1ResourcePolicyRuleBuilder item : resourceRules) {
        if (predicate.test(item)) {
          return true;
        }
      }
      return false;
  }
  
  public A withResourceRules(List<V1ResourcePolicyRule> resourceRules) {
    if (this.resourceRules != null) {
      this._visitables.get("resourceRules").clear();
    }
    if (resourceRules != null) {
        this.resourceRules = new ArrayList();
        for (V1ResourcePolicyRule item : resourceRules) {
          this.addToResourceRules(item);
        }
    } else {
      this.resourceRules = null;
    }
    return (A) this;
  }
  
  public A withResourceRules(io.kubernetes.client.openapi.models.V1ResourcePolicyRule... resourceRules) {
    if (this.resourceRules != null) {
        this.resourceRules.clear();
        _visitables.remove("resourceRules");
    }
    if (resourceRules != null) {
      for (V1ResourcePolicyRule item : resourceRules) {
        this.addToResourceRules(item);
      }
    }
    return (A) this;
  }
  
  public boolean hasResourceRules() {
    return this.resourceRules != null && !this.resourceRules.isEmpty();
  }
  
  public ResourceRulesNested<A> addNewResourceRule() {
    return new ResourceRulesNested(-1, null);
  }
  
  public ResourceRulesNested<A> addNewResourceRuleLike(V1ResourcePolicyRule item) {
    return new ResourceRulesNested(-1, item);
  }
  
  public ResourceRulesNested<A> setNewResourceRuleLike(int index,V1ResourcePolicyRule item) {
    return new ResourceRulesNested(index, item);
  }
  
  public ResourceRulesNested<A> editResourceRule(int index) {
    if (resourceRules.size() <= index) throw new RuntimeException("Can't edit resourceRules. Index exceeds size.");
    return setNewResourceRuleLike(index, buildResourceRule(index));
  }
  
  public ResourceRulesNested<A> editFirstResourceRule() {
    if (resourceRules.size() == 0) throw new RuntimeException("Can't edit first resourceRules. The list is empty.");
    return setNewResourceRuleLike(0, buildResourceRule(0));
  }
  
  public ResourceRulesNested<A> editLastResourceRule() {
    int index = resourceRules.size() - 1;
    if (index < 0) throw new RuntimeException("Can't edit last resourceRules. The list is empty.");
    return setNewResourceRuleLike(index, buildResourceRule(index));
  }
  
  public ResourceRulesNested<A> editMatchingResourceRule(Predicate<V1ResourcePolicyRuleBuilder> predicate) {
    int index = -1;
    for (int i=0;i<resourceRules.size();i++) { 
    if (predicate.test(resourceRules.get(i))) {index = i; break;}
    } 
    if (index < 0) throw new RuntimeException("Can't edit matching resourceRules. No match found.");
    return setNewResourceRuleLike(index, buildResourceRule(index));
  }
  
  public A addToSubjects(int index,FlowcontrolV1Subject item) {
    if (this.subjects == null) {this.subjects = new ArrayList<FlowcontrolV1SubjectBuilder>();}
    FlowcontrolV1SubjectBuilder builder = new FlowcontrolV1SubjectBuilder(item);
    if (index < 0 || index >= subjects.size()) { _visitables.get("subjects").add(builder); subjects.add(builder); } else { _visitables.get("subjects").add(index, builder); subjects.add(index, builder);}
    return (A)this;
  }
  
  public A setToSubjects(int index,FlowcontrolV1Subject item) {
    if (this.subjects == null) {this.subjects = new ArrayList<FlowcontrolV1SubjectBuilder>();}
    FlowcontrolV1SubjectBuilder builder = new FlowcontrolV1SubjectBuilder(item);
    if (index < 0 || index >= subjects.size()) { _visitables.get("subjects").add(builder); subjects.add(builder); } else { _visitables.get("subjects").set(index, builder); subjects.set(index, builder);}
    return (A)this;
  }
  
  public A addToSubjects(io.kubernetes.client.openapi.models.FlowcontrolV1Subject... items) {
    if (this.subjects == null) {this.subjects = new ArrayList<FlowcontrolV1SubjectBuilder>();}
    for (FlowcontrolV1Subject item : items) {FlowcontrolV1SubjectBuilder builder = new FlowcontrolV1SubjectBuilder(item);_visitables.get("subjects").add(builder);this.subjects.add(builder);} return (A)this;
  }
  
  public A addAllToSubjects(Collection<FlowcontrolV1Subject> items) {
    if (this.subjects == null) {this.subjects = new ArrayList<FlowcontrolV1SubjectBuilder>();}
    for (FlowcontrolV1Subject item : items) {FlowcontrolV1SubjectBuilder builder = new FlowcontrolV1SubjectBuilder(item);_visitables.get("subjects").add(builder);this.subjects.add(builder);} return (A)this;
  }
  
  public A removeFromSubjects(io.kubernetes.client.openapi.models.FlowcontrolV1Subject... items) {
    if (this.subjects == null) return (A)this;
    for (FlowcontrolV1Subject item : items) {FlowcontrolV1SubjectBuilder builder = new FlowcontrolV1SubjectBuilder(item);_visitables.get("subjects").remove(builder); this.subjects.remove(builder);} return (A)this;
  }
  
  public A removeAllFromSubjects(Collection<FlowcontrolV1Subject> items) {
    if (this.subjects == null) return (A)this;
    for (FlowcontrolV1Subject item : items) {FlowcontrolV1SubjectBuilder builder = new FlowcontrolV1SubjectBuilder(item);_visitables.get("subjects").remove(builder); this.subjects.remove(builder);} return (A)this;
  }
  
  public A removeMatchingFromSubjects(Predicate<FlowcontrolV1SubjectBuilder> predicate) {
    if (subjects == null) return (A) this;
    final Iterator<FlowcontrolV1SubjectBuilder> each = subjects.iterator();
    final List visitables = _visitables.get("subjects");
    while (each.hasNext()) {
      FlowcontrolV1SubjectBuilder builder = each.next();
      if (predicate.test(builder)) {
        visitables.remove(builder);
        each.remove();
      }
    }
    return (A)this;
  }
  
  public List<FlowcontrolV1Subject> buildSubjects() {
    return this.subjects != null ? build(subjects) : null;
  }
  
  public FlowcontrolV1Subject buildSubject(int index) {
    return this.subjects.get(index).build();
  }
  
  public FlowcontrolV1Subject buildFirstSubject() {
    return this.subjects.get(0).build();
  }
  
  public FlowcontrolV1Subject buildLastSubject() {
    return this.subjects.get(subjects.size() - 1).build();
  }
  
  public FlowcontrolV1Subject buildMatchingSubject(Predicate<FlowcontrolV1SubjectBuilder> predicate) {
      for (FlowcontrolV1SubjectBuilder item : subjects) {
        if (predicate.test(item)) {
          return item.build();
        }
      }
      return null;
  }
  
  public boolean hasMatchingSubject(Predicate<FlowcontrolV1SubjectBuilder> predicate) {
      for (FlowcontrolV1SubjectBuilder item : subjects) {
        if (predicate.test(item)) {
          return true;
        }
      }
      return false;
  }
  
  public A withSubjects(List<FlowcontrolV1Subject> subjects) {
    if (this.subjects != null) {
      this._visitables.get("subjects").clear();
    }
    if (subjects != null) {
        this.subjects = new ArrayList();
        for (FlowcontrolV1Subject item : subjects) {
          this.addToSubjects(item);
        }
    } else {
      this.subjects = null;
    }
    return (A) this;
  }
  
  public A withSubjects(io.kubernetes.client.openapi.models.FlowcontrolV1Subject... subjects) {
    if (this.subjects != null) {
        this.subjects.clear();
        _visitables.remove("subjects");
    }
    if (subjects != null) {
      for (FlowcontrolV1Subject item : subjects) {
        this.addToSubjects(item);
      }
    }
    return (A) this;
  }
  
  public boolean hasSubjects() {
    return this.subjects != null && !this.subjects.isEmpty();
  }
  
  public SubjectsNested<A> addNewSubject() {
    return new SubjectsNested(-1, null);
  }
  
  public SubjectsNested<A> addNewSubjectLike(FlowcontrolV1Subject item) {
    return new SubjectsNested(-1, item);
  }
  
  public SubjectsNested<A> setNewSubjectLike(int index,FlowcontrolV1Subject item) {
    return new SubjectsNested(index, item);
  }
  
  public SubjectsNested<A> editSubject(int index) {
    if (subjects.size() <= index) throw new RuntimeException("Can't edit subjects. Index exceeds size.");
    return setNewSubjectLike(index, buildSubject(index));
  }
  
  public SubjectsNested<A> editFirstSubject() {
    if (subjects.size() == 0) throw new RuntimeException("Can't edit first subjects. The list is empty.");
    return setNewSubjectLike(0, buildSubject(0));
  }
  
  public SubjectsNested<A> editLastSubject() {
    int index = subjects.size() - 1;
    if (index < 0) throw new RuntimeException("Can't edit last subjects. The list is empty.");
    return setNewSubjectLike(index, buildSubject(index));
  }
  
  public SubjectsNested<A> editMatchingSubject(Predicate<FlowcontrolV1SubjectBuilder> predicate) {
    int index = -1;
    for (int i=0;i<subjects.size();i++) { 
    if (predicate.test(subjects.get(i))) {index = i; break;}
    } 
    if (index < 0) throw new RuntimeException("Can't edit matching subjects. No match found.");
    return setNewSubjectLike(index, buildSubject(index));
  }
  
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    V1PolicyRulesWithSubjectsFluent that = (V1PolicyRulesWithSubjectsFluent) o;
    if (!java.util.Objects.equals(nonResourceRules, that.nonResourceRules)) return false;
    if (!java.util.Objects.equals(resourceRules, that.resourceRules)) return false;
    if (!java.util.Objects.equals(subjects, that.subjects)) return false;
    return true;
  }
  
  public int hashCode() {
    return java.util.Objects.hash(nonResourceRules,  resourceRules,  subjects,  super.hashCode());
  }
  
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    if (nonResourceRules != null && !nonResourceRules.isEmpty()) { sb.append("nonResourceRules:"); sb.append(nonResourceRules + ","); }
    if (resourceRules != null && !resourceRules.isEmpty()) { sb.append("resourceRules:"); sb.append(resourceRules + ","); }
    if (subjects != null && !subjects.isEmpty()) { sb.append("subjects:"); sb.append(subjects); }
    sb.append("}");
    return sb.toString();
  }
  public class NonResourceRulesNested<N> extends V1NonResourcePolicyRuleFluent<NonResourceRulesNested<N>> implements Nested<N>{
    NonResourceRulesNested(int index,V1NonResourcePolicyRule item) {
      this.index = index;
      this.builder = new V1NonResourcePolicyRuleBuilder(this, item);
    }
    V1NonResourcePolicyRuleBuilder builder;
    int index;
    
    public N and() {
      return (N) V1PolicyRulesWithSubjectsFluent.this.setToNonResourceRules(index,builder.build());
    }
    
    public N endNonResourceRule() {
      return and();
    }
    
  
  }
  public class ResourceRulesNested<N> extends V1ResourcePolicyRuleFluent<ResourceRulesNested<N>> implements Nested<N>{
    ResourceRulesNested(int index,V1ResourcePolicyRule item) {
      this.index = index;
      this.builder = new V1ResourcePolicyRuleBuilder(this, item);
    }
    V1ResourcePolicyRuleBuilder builder;
    int index;
    
    public N and() {
      return (N) V1PolicyRulesWithSubjectsFluent.this.setToResourceRules(index,builder.build());
    }
    
    public N endResourceRule() {
      return and();
    }
    
  
  }
  public class SubjectsNested<N> extends FlowcontrolV1SubjectFluent<SubjectsNested<N>> implements Nested<N>{
    SubjectsNested(int index,FlowcontrolV1Subject item) {
      this.index = index;
      this.builder = new FlowcontrolV1SubjectBuilder(this, item);
    }
    FlowcontrolV1SubjectBuilder builder;
    int index;
    
    public N and() {
      return (N) V1PolicyRulesWithSubjectsFluent.this.setToSubjects(index,builder.build());
    }
    
    public N endSubject() {
      return and();
    }
    
  
  }

}