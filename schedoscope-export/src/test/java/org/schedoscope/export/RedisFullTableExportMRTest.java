/**
 * Copyright 2016 Otto (GmbH & Co KG)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.schedoscope.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hive.hcatalog.mapreduce.HCatInputFormat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.rarefiedredis.redis.adapter.jedis.JedisAdapter;
import org.schedoscope.export.outputformat.RedisHashWritable;
import org.schedoscope.export.outputformat.RedisOutputFormat;
import org.schedoscope.export.utils.RedisMRJedisFactory;
import org.schedoscope.export.utils.RedisMRUtils;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.*", "org.apache.*", "com.*", "org.mortbay.*", "org.xml.*", "org.w3c.*"})
@PrepareForTest(RedisMRJedisFactory.class)
public class RedisFullTableExportMRTest extends HiveUnitBaseTest {

	JedisAdapter jedisAdapter;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		jedisAdapter = new JedisAdapter();
		PowerMockito.mockStatic(RedisMRJedisFactory.class);
		when(RedisMRJedisFactory.getJedisClient(any(Configuration.class))).thenReturn(jedisAdapter);
	}

	@Test
	public void testRedisFullExport() throws Exception {

		setUpHiveServer("src/test/resources/ogm_event_features_data.txt",
				"src/test/resources/ogm_event_features.hql",
				"ogm_event_features");

		final String KEY = "visitor_id";
		conf.set(RedisMRUtils.REDIS_EXPORT_KEY_PREFIX, "export1");
		conf.set(RedisMRUtils.REDIS_EXPORT_KEY_NAME, KEY);

		Job job = Job.getInstance(conf);

		job.setMapperClass(RedisFullTableExportMapper.class);
		job.setReducerClass(RedisExportReducer.class);
		job.setNumReduceTasks(1);
		job.setInputFormatClass(HCatInputFormat.class);
		job.setOutputFormatClass(RedisOutputFormat.class);

		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(RedisHashWritable.class);
		job.setOutputKeyClass(RedisHashWritable.class);
		job.setOutputValueClass(NullWritable.class);

		assertTrue(job.waitForCompletion(true));
		assertEquals("2016-02-09T12:21:24.581+01:00", jedisAdapter.hget("export1_0000e5da-0c7f-43d4-ba63-c7cd74eca7f6", "created_at"));
	}
}
