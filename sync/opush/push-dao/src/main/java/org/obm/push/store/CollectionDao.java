package org.obm.push.store;

import java.util.Date;

import org.obm.push.bean.ChangedCollections;
import org.obm.push.bean.Device;
import org.obm.push.bean.SyncState;
import org.obm.push.exception.CollectionNotFoundException;
import org.obm.push.exception.DaoException;

public interface CollectionDao {

	Integer addCollectionMapping(Device device, String collection) throws DaoException;

	int getCollectionMapping(Device device, String collectionId)
			throws CollectionNotFoundException, DaoException;

	String getCollectionPath(Integer collectionId)
			throws CollectionNotFoundException, DaoException;

	void resetCollection(Device device, Integer collectionId) throws DaoException;
	
	void updateState(Device device, Integer collectionId, SyncState state) throws DaoException;

	SyncState findStateForKey(String syncKey) throws DaoException, CollectionNotFoundException;
	
	ChangedCollections getCalendarChangedCollections(Date lastSync) throws DaoException;

	ChangedCollections getContactChangedCollections(Date lastSync) throws DaoException;
	
}
