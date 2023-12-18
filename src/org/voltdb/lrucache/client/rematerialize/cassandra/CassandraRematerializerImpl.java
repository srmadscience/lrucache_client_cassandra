package org.voltdb.lrucache.client.rematerialize.cassandra;

/* This file is part of VoltDB.
 * Copyright (C) 2008-2017 VoltDB Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

import java.util.Properties;

import org.voltdb.lrucache.client.rematerialize.AbstractSqlRematerializer;
import org.voltdb.lrucache.client.rematerialize.LRUCacheRematerializer;
import org.voltdb.seutils.wranglers.cassandra.CassandraWrangler;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

public class CassandraRematerializerImpl extends AbstractSqlRematerializer implements LRUCacheRematerializer {

	/**
	 * Used to manage connectivity to Cassandra
	 */
	CassandraWrangler m_cw = null;

	/**
	 * Set if something goes horribly wrong
	 */
	boolean m_isBroken = false;

	/**
	 * SQL statement we query Cassandra with
	 */
	String m_cassandraSqlQueryText = null;

	/**
	 * Cassandra Session
	 */
	Session m_cassandraSession;

	/**
	 * Prepared statements holding our SQL
	 */
	PreparedStatement m_ps;

	
	/**
	 * Non-argument constructor. Arguments are passed in via 'setConfig'
	 */
	public CassandraRematerializerImpl() {
		super();	
	}
	
	/* (non-Javadoc)
	 * @see org.voltdb.lrucache.client.rematerialize.AbstractSqlRematerializer#setConfig(java.lang.String, java.lang.String, java.util.Properties)
	 */
	public void setConfig (String schemaName, String tableName, Properties config) {
		
		super.setConfig(schemaName, tableName, config);

		m_cw = new CassandraWrangler(m_otherDbHostnames, m_otherDbUsername, m_otherDbPassword, m_otherDbPort, config);

		if (m_cw.confirmConnected()) {

			m_cassandraSession = m_cw.getSession();

		}

	}

	/**
	 * Implementing classes use this method to find 'missing' data.
	 * 
	 * @param pkArgs
	 *            The Primary Key fields
	 * @param m_pkArgCount
	 *            How any of the PK fields are actually the PK
	 * @return Object[] of values in VoltDB column order, or null.
	 * @throws Exception
	 */
	@Override
	public synchronized Object[] fetch(Object[] pkArgs, int pkArgCount) throws Exception {
		
		final long start = System.currentTimeMillis();
		Object[] data = null;

		try {
			if (m_cw.confirmConnected()) {

				// Get SQL text
				if (m_cassandraSqlQueryText == null) {
					m_cassandraSqlQueryText = getSelectSql(CASSANDRA);
				}

				// Get prepared statement.
				if (m_ps == null) {
					m_ps = m_cassandraSession.prepare(m_cassandraSqlQueryText);
				}

				// Cassandra also needs a 'bound statement'
				BoundStatement bs = m_ps.bind();

				for (int i = 0; i < pkArgs.length && i < pkArgCount; i++) {
					String pkColName = getPkCol(i + 1);
					CassandraWrangler.safeCassandraBind(bs, i, pkArgs[i], getColDatatype(pkColName));
				}

				// Do our read and track the many bad possible outcomes...
				try {
					ResultSet rs = m_cassandraSession.execute(bs);
					m_statsInstance.reportLatency(CASSANDRA + "_query_ms", start, "", 100);

					Row row = rs.one();

					data = CassandraWrangler.mapCassandraRowToVoltObjectArray(row);
					

				} catch (com.datastax.driver.core.exceptions.OperationTimedOutException e) {

					m_statsInstance.reportLatency(CASSANDRA + "_query_timeout", start, e.getMessage(), 100);
					
				} catch (com.datastax.driver.core.exceptions.TransportException e) {

					m_statsInstance.reportLatency(CASSANDRA + "_transport_exception", start, e.getMessage(), 100);			
					throw (e);

				} catch (com.datastax.driver.core.exceptions.NoHostAvailableException e) {

					m_statsInstance.reportLatency(CASSANDRA + "_nohost_exception", start, e.getMessage(), 100);		
					throw (e);
				}

			} else {
				
				logger.error("fetch called when not connected");
				throw new Exception("fetch called when not connected");
				
			}
			
		} catch (Exception e) {
			
			m_isBroken = true;
			logger.error(e.getMessage());
			logStackTrace("CassandraRematerializerImpl.fetch", logger, e);
			disconnect();
		}

	
		return data;
	}

	@Override
	public void disconnect() {

		super.disconnect();

		m_ps = null;
		
		m_cassandraSession.close();

		if (m_cw != null) {
			m_cw.disconnect();
		}
	}

}
