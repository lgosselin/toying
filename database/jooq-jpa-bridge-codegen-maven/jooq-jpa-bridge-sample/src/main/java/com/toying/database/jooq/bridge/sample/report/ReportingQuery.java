package com.toying.database.jooq.bridge.sample.report;

import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;

import com.toying.database.meta.jooq.tables.TableA;
import com.toying.database.meta.jooq.tables.TableB;

/* Not a very bright example, just enough to put some JOOQ code which cannot possibly compile
 * without the generated code. This is just a POC */
public class ReportingQuery {
	
	public static void dosomething(String someDataPrefix) {
		DSLContext context = getDSLContext();
		Result<?> results = context.selectDistinct(TableA.TABLE_A.EXTERNAL_ID)
				.from(TableA.TABLE_A).join(TableB.TABLE_B).on(DSL.condition("table_b.parent_id = table_a.id"))
				.where(DSL.condition("someData LIKE '" + someDataPrefix + "%'"))
				.fetch();
		results.getValues(TableA.TABLE_A.EXTERNAL_ID);
	}

	/* TODO: Access a central place (or inject a component) which can create a properly initialized context.
	 * Any attempt to use this one will fail miserably. */
	private static DSLContext getDSLContext() {
		return DSL.using(new DefaultConfiguration());
	}
}
