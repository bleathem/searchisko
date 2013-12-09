/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.persistence.service;

import java.util.List;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Test;
import org.searchisko.persistence.jpa.model.Rating;
import org.searchisko.persistence.service.RatingPersistenceService.RatingStats;

/**
 * Unit test for {@link JpaRatingPersistenceService}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class JpaRatingPersistenceServiceTest extends JpaTestBase {
	private static final String CONTENT_ID_3 = "contId3";
	private static final String CONTENT_ID_2 = "contId2";
	private static final String CONTENT_ID_1 = "contId1";
	private static final String CONTRIB_ID_2 = "contribId2";
	private static final String CONTRIB_ID_1 = "contribId1";

	{
		logger = Logger.getLogger(JpaRatingPersistenceServiceTest.class.getName());
	}

	@Test
	public void getRatings_rate() {
		JpaRatingPersistenceService tested = getTested();

		// case - get from empty store
		em.getTransaction().begin();
		List<Rating> ret = tested.getRatings(CONTRIB_ID_1, CONTENT_ID_1);
		Assert.assertTrue(ret.isEmpty());

		ret = tested.getRatings(CONTRIB_ID_1, CONTENT_ID_1, CONTENT_ID_2, CONTENT_ID_3);
		Assert.assertTrue(ret.isEmpty());

		// case - rate some content
		tested.rate(CONTRIB_ID_1, CONTENT_ID_1, 1);
		tested.rate(CONTRIB_ID_2, CONTENT_ID_1, 5);

		tested.rate(CONTRIB_ID_1, CONTENT_ID_3, 4);
		em.getTransaction().commit();

		em.getTransaction().begin();
		// case - get rated content
		ret = tested.getRatings(CONTRIB_ID_1, CONTENT_ID_2);
		Assert.assertEquals(0, ret.size());

		ret = tested.getRatings(CONTRIB_ID_1, CONTENT_ID_1);
		Assert.assertEquals(1, ret.size());

		ret = tested.getRatings(CONTRIB_ID_1, CONTENT_ID_1, CONTENT_ID_2, CONTENT_ID_3);
		Assert.assertEquals(2, ret.size());

		ret = tested.getRatings(CONTRIB_ID_2, CONTENT_ID_1, CONTENT_ID_2, CONTENT_ID_3);
		Assert.assertEquals(1, ret.size());
		Assert.assertEquals(CONTENT_ID_1, ret.get(0).getContentId());
		Assert.assertEquals(CONTRIB_ID_2, ret.get(0).getContributorId());
		Assert.assertEquals(5, ret.get(0).getRating());
		Assert.assertNotNull(ret.get(0).getRatedAt());

		// case - update some rating
		tested.rate(CONTRIB_ID_2, CONTENT_ID_1, 2);
		em.getTransaction().commit();

		em.getTransaction().begin();
		ret = tested.getRatings(CONTRIB_ID_2, CONTENT_ID_1, CONTENT_ID_2, CONTENT_ID_3);
		Assert.assertEquals(1, ret.size());
		Assert.assertEquals(CONTENT_ID_1, ret.get(0).getContentId());
		Assert.assertEquals(CONTRIB_ID_2, ret.get(0).getContributorId());
		Assert.assertEquals(2, ret.get(0).getRating());
		Assert.assertNotNull(ret.get(0).getRatedAt());
		em.getTransaction().commit();

	}

	protected JpaRatingPersistenceService getTested() {
		JpaRatingPersistenceService tested = new JpaRatingPersistenceService();
		tested.em = em;
		return tested;
	}

	@Test
	public void countRatingStats() {
		JpaRatingPersistenceService tested = getTested();

		// create some content
		em.getTransaction().begin();
		tested.rate(CONTRIB_ID_1, CONTENT_ID_1, 1);
		tested.rate(CONTRIB_ID_2, CONTENT_ID_1, 5);
		tested.rate(CONTRIB_ID_1, CONTENT_ID_3, 4);
		em.getTransaction().commit();

		em.getTransaction().begin();

		RatingStats rs = tested.countRatingStats(CONTENT_ID_1);
		Assert.assertEquals(CONTENT_ID_1, rs.getContentId());
		Assert.assertEquals(3, rs.getAverage(), 0.01);
		Assert.assertEquals(2, rs.getNumber());

		// case - unrated content
		Assert.assertNull(tested.countRatingStats(CONTENT_ID_2));

		rs = tested.countRatingStats(CONTENT_ID_3);
		Assert.assertEquals(CONTENT_ID_3, rs.getContentId());
		Assert.assertEquals(4, rs.getAverage(), 0.01);
		Assert.assertEquals(1, rs.getNumber());

		em.getTransaction().commit();
	}

}
