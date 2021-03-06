/*
 * [New BSD License]
 * Copyright (c) 2011-2012, Brackit Project Team <info@brackit.org>  
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Brackit Project Team nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.brackit.server.store.index.bracket;

import org.brackit.server.node.DocID;
import org.brackit.server.node.XTCdeweyID;
import org.brackit.server.store.SearchMode;

/**
 * @author Martin Hiller
 *
 */
public enum NavigationMode {

	NEXT_SIBLING(SearchMode.GREATEST_HAVING_PREFIX) {
		@Override
		public XTCdeweyID getSearchKey(XTCdeweyID referenceKey)
		{
			return referenceKey;
		}

		@Override
		public boolean isAfterHighKey(XTCdeweyID referenceKey,
				XTCdeweyID highKey)
		{
			return referenceKey.isPrefixOrGreater(highKey);
		}
	},
	PREVIOUS_SIBLING(SearchMode.LESS) {
		@Override
		public XTCdeweyID getSearchKey(XTCdeweyID referenceKey)
		{
			return referenceKey;
		}

		@Override
		public boolean isAfterHighKey(XTCdeweyID referenceKey,
				XTCdeweyID highKey)
		{
			return (referenceKey.compareReduced(highKey) > 0);
		}
	},
	FIRST_CHILD(SearchMode.GREATEST_HAVING_PREFIX) {
		@Override
		public XTCdeweyID getSearchKey(XTCdeweyID referenceKey)
		{
			return referenceKey.getAttributeRootID();
		}

		@Override
		public boolean isAfterHighKey(XTCdeweyID referenceKey,
				XTCdeweyID highKey)
		{
			// return true if referenceKey's attribute root is a prefix or greater than the highKey
			return referenceKey.isPrefixOrGreater(1, highKey);
		}
	},
	LAST_CHILD(SearchMode.GREATEST_HAVING_PREFIX) {
		@Override
		public XTCdeweyID getSearchKey(XTCdeweyID referenceKey)
		{
			return referenceKey;
		}

		@Override
		public boolean isAfterHighKey(XTCdeweyID referenceKey,
				XTCdeweyID highKey)
		{
			return referenceKey.isPrefixOrGreater(highKey);
		}
	},
	PARENT(SearchMode.GREATER) {
		@Override
		public XTCdeweyID getSearchKey(XTCdeweyID referenceKey)
		{
			return referenceKey.getParent();
		}

		@Override
		public boolean isAfterHighKey(XTCdeweyID referenceKey,
				XTCdeweyID highKey)
		{
			return (referenceKey.compareParentTo(highKey) >= 0);
		}
	},
	TO_INSERT_POS(SearchMode.GREATER) {
		@Override
		public XTCdeweyID getSearchKey(XTCdeweyID referenceKey)
		{
			return referenceKey;
		}
		
		@Override
		public boolean isAfterHighKey(XTCdeweyID referenceKey,
				XTCdeweyID highKey)
		{
			return (referenceKey.compareReduced(highKey) >= 0);
		}
	},
	TO_KEY(SearchMode.GREATER) {
		@Override
		public XTCdeweyID getSearchKey(XTCdeweyID referenceKey)
		{
			return referenceKey;
		}

		@Override
		public boolean isAfterHighKey(XTCdeweyID referenceKey,
				XTCdeweyID highKey)
		{
			return (referenceKey.compareReduced(highKey) >= 0);
		}
	},
	NEXT_ATTRIBUTE(SearchMode.GREATER) {
		@Override
		public XTCdeweyID getSearchKey(XTCdeweyID referenceKey)
		{
			return referenceKey;
		}
		
		@Override
		public boolean isAfterHighKey(XTCdeweyID referenceKey,
				XTCdeweyID highKey)
		{
			return (referenceKey.compareReduced(highKey) >= 0);
		}
	},
	LAST(SearchMode.LAST) {
		
		private final XTCdeweyID searchKey = new XTCdeweyID(new DocID(Integer.MAX_VALUE, 0));
		
		@Override
		public XTCdeweyID getSearchKey(XTCdeweyID referenceKey)
		{
			return searchKey;
		}
		
		@Override
		public boolean isAfterHighKey(XTCdeweyID referenceKey,
				XTCdeweyID highKey)
		{
			return (referenceKey.compareReduced(highKey) >= 0);
		}
	},
	NEXT_DOCUMENT(SearchMode.GREATER) {
		@Override
		public XTCdeweyID getSearchKey(XTCdeweyID referenceKey)
		{
			return new XTCdeweyID(new DocID(referenceKey.docID.getCollectionID(), referenceKey.docID.getDocNumber() + 1));
		}

		@Override
		public boolean isAfterHighKey(XTCdeweyID referenceKey,
				XTCdeweyID highKey)
		{
			return referenceKey.docID.compareTo(highKey.docID) >= 0;
		}
	};
	
	private SearchMode searchMode;
	
	private NavigationMode(SearchMode searchMode) {
		this.searchMode = searchMode;
	}
	
	public SearchMode getSearchMode() {
		return searchMode;
	}
	
	public abstract XTCdeweyID getSearchKey(XTCdeweyID referenceKey);
	
	/**
	 * This check utilizes the highkeys stored in the leaf pages.
	 * It returns true if the current leaf page can be skipped during this navigation operation.
	 * @param referenceKey the reference key of the current navigation operation
	 * @param highKey the current leaf page's highKey
	 * @return true if target node certainly lies in the next page(s)
	 */
	public abstract boolean isAfterHighKey(XTCdeweyID referenceKey, XTCdeweyID highKey);
}
