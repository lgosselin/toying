package com.toying.database.jooq.bridge.sample.persistence;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.*;

@Entity
@Table(
		name="table_a", 
		uniqueConstraints={@UniqueConstraint(columnNames={"external_id"})}
)
public class A {
	private Long id;
	private String externalId;
	private Set<B> children;
	
	protected A() {
	}
	
	public A(String externalId) {
		this.externalId = externalId;
		children = new HashSet<B>();
	}

	@Id 
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	@Column(name="external_id", nullable=false, insertable=true, updatable=false)
	public String getExternalId() {
		return externalId;
	}
	
	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}

	@OneToMany(mappedBy="pk.parent")
	public Set<B> getChildren() {
		return children;
	}
	
	public void setChildren(Set<B> children) {
		this.children = children;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result	+ ((externalId == null) ? 0 : externalId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof A))
			return false;
		A other = (A) obj;
		if (externalId == null) {
			if (other.externalId != null)
				return false;
		} else if (!externalId.equals(other.externalId))
			return false;
		if (children == null) {
			if (other.children != null)
				return false;
		} else if (!children.equals(other.children))
			return false;
		return true;
	}
	
}
