# FastqHeaderNormalizer

## Description
The **FastqHeaderNormalizer** plugin parses and normalizes FASTQ sequencing headers into a structured schema. It supports multiple header formats including Illumina Casava 1.8+, Legacy Illumina, and Oxford Nanopore.

## Configuration

| Property | Type | Description |
|Str|Str|Str|
| **headerFormat** | String | The expected format of the FASTQ header. Options: `Auto-Detect`, `Illumina Casava 1.8+`, `Legacy Illumina`, `Oxford Nanopore`. Defaults to `Auto-Detect`. |
| **sequence_header** | String | The name of the input field containing the FASTQ header string. Defaults to `sequence_header`. |

## Example
This plugin transforms a raw header string into structured fields.

**Input Record:**
```json
{
  "sequence_header": "@EAS139:136:FC706VJ:2:2104:15343:197393 1:Y:18:ATCACG"
}
```

**Output Record:**
```json
{
  "original_header": "@EAS139:136:FC706VJ:2:2104:15343:197393 1:Y:18:ATCACG",
  "instrument_id": "EAS139",
  "run_id": "136",
  "flowcell_id": "FC706VJ",
  "lane": 2,
  "tile": 2104,
  "x_pos": 15343,
  "y_pos": 197393,
  "read_num": 1,
  "is_filtered": true,
  "control_num": 18,
  "index_sequence": "ATCACG",
  "is_malformed": false
}
```
