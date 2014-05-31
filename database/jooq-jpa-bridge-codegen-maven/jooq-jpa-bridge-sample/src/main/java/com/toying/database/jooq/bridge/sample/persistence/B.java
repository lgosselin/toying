package com.toying.database.jooq.bridge.sample.persistence;

import java.io.Serializable;

import javax.persistence.*;

import org.hibernate.annotations.ForeignKey;

@Entity
@Table(
		name="table_b", 
		uniqueConstraints={@UniqueConstraint(columnNames = {"parent_id", "external_id"})}
)
public class B {
	private PK pk;
	private String someData;
	
	protected B() {
	}
	
	public B(A parent, String externalId, String someData) {
		this.pk = new PK(parent, externalId);
		this.someData = someData;
	}

	@EmbeddedId
	public PK getPk() {
		return pk;
	}

	public void setPk(PK pk) {
		this.pk = pk;
	}

	@Column(name="some_data")
	public String getSomeData() {
		return someData;
	}

	public void setSomeData(String someData) {
		this.someData = someData;
	}

	@Embeddable
	public static class PK implements Serializable {
		private static final long serialVersionUID = 1L;
		private A parent;
		private String externalId;
		
		protected PK() {
		}
		
		public PK(A parent, String externalId) {
			this.parent = parent;
			this.externalId = externalId;
		}

		@ManyToOne(optional=false)
		@ForeignKey(name="FK_B_parent_A_id") 
		public A getParent() {
			return parent;
		}

		public void setParent(A parent) {
			this.parent = parent;
		}

		@Column(name="external_id", nullable=false)
		public String getExternalId() {
			return externalId;
		}

		public void setExternalId(String externalId) {
			this.externalId = externalId;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((externalId == null) ? 0 : externalId.hashCode());
			result = prime * result
					+ ((parent == null) ? 0 : parent.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof PK))
				return false;
			PK other = (PK) obj;
			if (externalId == null) {
				if (other.externalId != null)
					return false;
			} else if (!externalId.equals(other.externalId))
				return false;
			if (parent == null) {
				if (other.parent != null)
					return false;
			} else if (!parent.equals(other.parent))
				return false;
			return true;
		}
	}
}
