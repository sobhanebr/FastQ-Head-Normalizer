package io.cdap.plugin.fastq;

import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Macro;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.annotation.Plugin;
import io.cdap.cdap.api.data.format.StructuredRecord;
import io.cdap.cdap.api.data.schema.Schema;
import io.cdap.cdap.api.plugin.PluginConfig;
import io.cdap.cdap.etl.api.Emitter;
import io.cdap.cdap.etl.api.PipelineConfigurer;
import io.cdap.cdap.etl.api.Transform;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

@Plugin(type = Transform.PLUGIN_TYPE)
@Name("FastqHeaderNormalizer")
@Description("Normalizes various FASTQ header formats into a structured schema.")
public class FastqHeaderNormalizer extends Transform<StructuredRecord, StructuredRecord> {

  private static final Schema OUTPUT_SCHEMA = Schema.recordOf("FastqHeader",
    Schema.Field.of("original_header", Schema.of(Schema.Type.STRING)),
    Schema.Field.of("instrument_id", Schema.nullableOf(Schema.of(Schema.Type.STRING))),
    Schema.Field.of("run_id", Schema.nullableOf(Schema.of(Schema.Type.STRING))),
    Schema.Field.of("flowcell_id", Schema.nullableOf(Schema.of(Schema.Type.STRING))),
    Schema.Field.of("lane", Schema.nullableOf(Schema.of(Schema.Type.INT))),
    Schema.Field.of("tile", Schema.nullableOf(Schema.of(Schema.Type.INT))),
    Schema.Field.of("x_pos", Schema.nullableOf(Schema.of(Schema.Type.INT))),
    Schema.Field.of("y_pos", Schema.nullableOf(Schema.of(Schema.Type.INT))),
    Schema.Field.of("read_num", Schema.nullableOf(Schema.of(Schema.Type.INT))),
    Schema.Field.of("is_filtered", Schema.nullableOf(Schema.of(Schema.Type.BOOLEAN))),
    Schema.Field.of("control_num", Schema.nullableOf(Schema.of(Schema.Type.INT))),
    Schema.Field.of("index_sequence", Schema.nullableOf(Schema.of(Schema.Type.STRING))),
    Schema.Field.of("nanopore_read_id", Schema.nullableOf(Schema.of(Schema.Type.STRING))),
    Schema.Field.of("nanopore_channel", Schema.nullableOf(Schema.of(Schema.Type.INT))),
    Schema.Field.of("is_malformed", Schema.of(Schema.Type.BOOLEAN))
  );

  // Regex Patterns
  private static final Pattern ILLUMINA_1_8_PATTERN = Pattern.compile(
      "^@([a-zA-Z0-9]+):([0-9]+):([a-zA-Z0-9]+):([0-9]+):([0-9]+):([0-9]+):([0-9]+)\\s+([0-9]+):([YN]):([0-9]+):([ACGTN]+)$");
  
  private static final Pattern ILLUMINA_OLD_PATTERN = Pattern.compile(
      "^@([a-zA-Z0-9-]+):([0-9]+):([0-9]+):([0-9]+):([0-9]+)#([0-9]+)/([0-9]+)$");

  private static final Pattern NANOPORE_PATTERN = Pattern.compile(
      "^@([a-f0-9-]+)\\s+runid=([a-zA-Z0-9]+)\\s+sampleid=([a-zA-Z0-9]+)\\s+read=([0-9]+)\\s+ch=([0-9]+)\\s+start_time=(.+)$");

  private final Config config;

  public FastqHeaderNormalizer(Config config) {
    this.config = config;
  }

  @Override
  public void configurePipeline(PipelineConfigurer pipelineConfigurer) {
    pipelineConfigurer.getStageConfigurer().setOutputSchema(OUTPUT_SCHEMA);
  }

  @Override
  public void transform(StructuredRecord input, Emitter<StructuredRecord> emitter) {
    // Read from the user-configured field
    // We use safe handling to ensure we don't crash on missing fields
    String fieldName = config.sequence_header;
    Object val = input.get(fieldName);
    String header = val instanceof String ? (String) val : null;
    
    if (header == null) {
      // Input was null or not a string, emit malformed
      emitter.emit(StructuredRecord.builder(OUTPUT_SCHEMA)
        .set("is_malformed", true)
        .build());
      return;
    }

    StructuredRecord.Builder builder = StructuredRecord.builder(OUTPUT_SCHEMA);
    builder.set("original_header", header);
    builder.set("is_malformed", false);

    // Try Illumina 1.8+
    Matcher m1 = ILLUMINA_1_8_PATTERN.matcher(header);
    if (m1.matches()) {
       builder.set("instrument_id", m1.group(1));
       builder.set("run_id", m1.group(2));
       builder.set("flowcell_id", m1.group(3));
       builder.set("lane", Integer.parseInt(m1.group(4)));
       builder.set("tile", Integer.parseInt(m1.group(5)));
       builder.set("x_pos", Integer.parseInt(m1.group(6)));
       builder.set("y_pos", Integer.parseInt(m1.group(7)));
       builder.set("read_num", Integer.parseInt(m1.group(8)));
       builder.set("is_filtered", "Y".equals(m1.group(9)));
       builder.set("control_num", Integer.parseInt(m1.group(10)));
       builder.set("index_sequence", m1.group(11));
       emitter.emit(builder.build());
       return;
    }

    // Try Old Illumina
    Matcher m2 = ILLUMINA_OLD_PATTERN.matcher(header);
    if (m2.matches()) {
       builder.set("instrument_id", m2.group(1));
       builder.set("lane", Integer.parseInt(m2.group(2)));
       builder.set("tile", Integer.parseInt(m2.group(3)));
       builder.set("x_pos", Integer.parseInt(m2.group(4)));
       builder.set("y_pos", Integer.parseInt(m2.group(5)));
       emitter.emit(builder.build());
       return;
    }

    // Try Nanopore
    Matcher m3 = NANOPORE_PATTERN.matcher(header);
    if (m3.matches()) {
       builder.set("nanopore_read_id", m3.group(1));
       builder.set("run_id", m3.group(2));
       builder.set("nanopore_channel", Integer.parseInt(m3.group(5)));
       emitter.emit(builder.build());
       return;
    }

    // If no match found
    builder.set("is_malformed", true);
    emitter.emit(builder.build());
  }

  public static class Config extends PluginConfig {
    @Name("sequence_header")
    @Description("The name of the input field containing the FASTQ header string.")
    @Macro
    public String sequence_header;
    
    @Name("headerFormat")
    @Description("The expected format or 'Auto-Detect'.")
    @Macro
    @Nullable
    public String headerFormat;
  }
}