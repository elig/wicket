/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.wicket.pageStore.memory;

import javax.servlet.http.HttpSession;

import org.apache.wicket.page.IPageManagerContext;
import org.apache.wicket.pageStore.IDataStore;

/**
 * A {@link DataStore} which stores the pages in the {@link HttpSession}. Uses
 * {@link DataStoreEvictionStrategy} to keep the memory footprint reasonable.
 * 
 * <p>
 * Usage:
 * 
 * <pre>
 * <!--@formatter:off-->
 * MyApp#init()
 * {
 * 
 * 	setPageManagerProvider(new DefaultPageManagerProvider() 
 * 	{
 * 		public IPageManager get(IPageManagerContext pageManagerContext) 
 * 		{ 
 * 			IDataStore dataStore = new HttpSessionDataStore(pageManagerContext, new PageNumberEvictionStrategy(20)); 
 * 			IPageStore pageStore = new DefaultPageStore(application.getName(), dataStore, getCacheSize()); 
 * 			return new PersistentPageManager(application.getName(), pageStore, pageManagerContext);
 * 		}
 * 	}
 * }
 * <!--@formatter:on-->
 * </pre>
 */
public class HttpSessionDataStore implements IDataStore
{

	/** the session attribute key. auto-prefixed with application.getSessionAttributePrefix() */
	private static final String PAGE_TABLE_KEY = "page:store:memory";

	private final IPageManagerContext pageManagerContext;

	private final DataStoreEvictionStrategy evictionStrategy;

	/**
	 * Construct.
	 * 
	 * @param pageManagerContext
	 * @param evictionStrategy
	 */
	public HttpSessionDataStore(IPageManagerContext pageManagerContext,
		DataStoreEvictionStrategy evictionStrategy)
	{
		this.pageManagerContext = pageManagerContext;
		this.evictionStrategy = evictionStrategy;
	}

	/**
	 * @param sessionId
	 *            Ignored. Only pages from the current http session can be read
	 * @see org.apache.wicket.pageStore.IDataStore#getData(java.lang.String, int)
	 */
	public byte[] getData(String sessionId, int pageId)
	{
		PageTable pageTable = getPageTable(false);
		byte[] pageAsBytes = null;
		if (pageTable != null)
		{
			pageAsBytes = pageTable.getPage(pageId);
		}
		return pageAsBytes;
	}

	public void removeData(String sessionId, int pageId)
	{
		PageTable pageTable = getPageTable(false);
		if (pageTable != null)
		{
			pageTable.removePage(pageId);
		}
	}

	public void removeData(String sessionId)
	{
		PageTable pageTable = getPageTable(false);
		if (pageTable != null)
		{
			pageTable.clear();
		}
	}

	public void storeData(String sessionId, int pageId, byte[] pageAsBytes)
	{
		PageTable pageTable = getPageTable(true);
		pageTable.storePage(pageId, pageAsBytes);

		evictionStrategy.evict(pageTable);
	}

	public void destroy()
	{
		PageTable pageTable = getPageTable(false);
		if (pageTable != null)
		{
			pageTable.clear();
		}
	}

	public boolean isReplicated()
	{
		return true;
	}

	private PageTable getPageTable(boolean create)
	{
		PageTable pageTable = (PageTable)pageManagerContext.getSessionAttribute(PAGE_TABLE_KEY);
		if (pageTable == null && create)
		{
			pageTable = new PageTable();
			pageManagerContext.setSessionAttribute(PAGE_TABLE_KEY, pageTable);
		}

		return pageTable;
	}

}
