package io.cdap.plugin.fastq;

import io.cdap.cdap.api.data.format.StructuredRecord;
import io.cdap.cdap.api.data.schema.Schema;
import io.cdap.cdap.etl.mock.common.MockEmitter;
import io.cdap.cdap.etl.mock.common.MockPipelineConfigurer;
import io.cdap.cdap.etl.mock.transform.MockTransformContext;
import org.junit.Assert;
import org.junit.Test;
import java.util.List;

public class FastqHeaderNormalizerTest {

  // Define input schema with the field name matching the configuration
  private static final Schema INPUT_SCHEMA = Schema.recordOf("input",
      Schema.Field.of("sequence_header", Schema.of(Schema.Type.STRING))
  );

  @Test
  public void testTransformFormats() throws Exception {
    // 1. Setup Config
    FastqHeaderNormalizer.Config config = new FastqHeaderNormalizer.Config();
    // Set the config property to match the schema field name
    config.sequence_header = "sequence_header";
    config.headerFormat = "Auto-Detect";

    // 2. Initialize Transform
    FastqHeaderNormalizer transform = new FastqHeaderNormalizer(config);
    transform.configurePipeline(new MockPipelineConfigurer(INPUT_SCHEMA));
    transform.initialize(new MockTransformContext());

    MockEmitter<StructuredRecord> emitter = new MockEmitter<>();

    // 3. Test Illumina 1.8+
    String illumina18 = "@EAS139:136:FC706VJ:2:2104:15343:197393 1:Y:18:ATCACG";
    transform.transform(StructuredRecord.builder(INPUT_SCHEMA).set("sequence_header", illumina18).build(), emitter);
    
    List<StructuredRecord> results = emitter.getEmitted();
    Assert.assertEquals(1, results.size());
    StructuredRecord record1 = results.get(0);
    Assert.assertEquals("EAS139", record1.get("instrument_id"));
    Assert.assertEquals("136", record1.get("run_id"));
    Assert.assertEquals((Integer) 2, record1.get("lane"));
    Assert.assertEquals("ATCACG", record1.get("index_sequence"));
    Assert.assertFalse(record1.get("is_malformed"));
    
    emitter.clear();

    // 4. Test Old Illumina
    String illuminaOld = "@HWUSI-EAS100R:6:73:941:1973#0/1";
    transform.transform(StructuredRecord.builder(INPUT_SCHEMA).set("sequence_header", illuminaOld).build(), emitter);
    
    results = emitter.getEmitted();
    Assert.assertEquals(1, results.size());
    StructuredRecord record2 = results.get(0);
    Assert.assertEquals("HWUSI-EAS100R", record2.get("instrument_id"));
    Assert.assertEquals((Integer) 6, record2.get("lane"));
    Assert.assertEquals((Integer) 73, record2.get("tile"));
    
    emitter.clear();

    // 5. Test Nanopore
    String nanopore = "@4d99d146-523c-4034-942d-20d04da4b46c runid=50953600 sampleid=0 read=327 ch=159 start_time=2020-09-24T18:31:07Z";
    transform.transform(StructuredRecord.builder(INPUT_SCHEMA).set("sequence_header", nanopore).build(), emitter);
    
    results = emitter.getEmitted();
    Assert.assertEquals(1, results.size());
    StructuredRecord record3 = results.get(0);
    Assert.assertEquals("4d99d146-523c-4034-942d-20d04da4b46c", record3.get("nanopore_read_id"));
    Assert.assertEquals("50953600", record3.get("run_id"));
    Assert.assertEquals((Integer) 159, record3.get("nanopore_channel"));
  }
}