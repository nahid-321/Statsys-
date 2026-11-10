package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testSampleDatasetGeneration() {
    val dataset = com.example.engine.SampleDataGenerator.createMasterDataset()
    assertEquals(122, dataset.rowCount)
    assertTrue(dataset.variables.size >= 14)
    assertEquals("P001", dataset.rows[0][0])
  }

  @Test
  fun testDescriptivesCalculation() {
    val dataset = com.example.engine.SampleDataGenerator.createMasterDataset()
    val output = com.example.engine.StatsEngine.computeDescriptives(dataset, listOf("Age_years"))
    assertEquals(1, output.statsList.size)
    val ageStats = output.statsList[0]
    assertTrue(ageStats.mean > 18.0)
    assertTrue(ageStats.n > 0)
  }

  @Test
  fun testSyntaxExecution() {
    val dataset = com.example.engine.SampleDataGenerator.createMasterDataset()
    val (newDs, outputs) = com.example.engine.SyntaxInterpreter.execute(
      "DESCRIPTIVES VARIABLES=Age_years Height_m Weight_kg.",
      dataset
    )
    assertTrue(outputs.isNotEmpty())
  }

  @Test
  fun testCsvParsingAndExport() {
    val csvData = "ID,Age,Score\n1,25,88.5\n2,30,92.0\n3,45,76.5\n"
    val parsed = com.example.engine.DatasetFileManager.parseDelimitedText(csvData, "test.csv")
    assertNotNull(parsed)
    assertEquals(3, parsed!!.rowCount)
    assertEquals(3, parsed.variables.size)
    assertEquals(com.example.model.VariableType.NUMERIC, parsed.variables[1].type)

    val exported = com.example.engine.DatasetFileManager.exportToCsv(parsed)
    assertTrue(exported.contains("ID,Age,Score"))
    assertTrue(exported.contains("1,25,88.5"))
  }


  @Test
  fun testXlsxStreamImport() {
    val baos = java.io.ByteArrayOutputStream()
    val zos = java.util.zip.ZipOutputStream(baos)
    
    // Add sharedStrings.xml
    zos.putNextEntry(java.util.zip.ZipEntry("xl/sharedStrings.xml"))
    val ssXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="3" uniqueCount="3">
  <si><t>Patient_ID</t></si>
  <si><t>Age</t></si>
  <si><t>P001</t></si>
</sst>"""
    zos.write(ssXml.toByteArray())
    zos.closeEntry()

    // Add sheet1.xml
    zos.putNextEntry(java.util.zip.ZipEntry("xl/worksheets/sheet1.xml"))
    val sheetXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>
    <row r="1">
      <c r="A1" t="s"><v>0</v></c>
      <c r="B1" t="s"><v>1</v></c>
    </row>
    <row r="2">
      <c r="A2" t="s"><v>2</v></c>
      <c r="B2"><v>28</v></c>
    </row>
  </sheetData>
</worksheet>"""
    zos.write(sheetXml.toByteArray())
    zos.closeEntry()
    zos.close()

    val xlsxBytes = baos.toByteArray()
    val dataset = com.example.engine.DatasetFileManager.importStream(java.io.ByteArrayInputStream(xlsxBytes), "test_file.xlsx")
    assertNotNull(dataset)
    assertEquals(2, dataset!!.variables.size)
    assertEquals("Patient_ID", dataset.variables[0].name)
    assertEquals("Age", dataset.variables[1].name)
    assertEquals(1, dataset.rowCount)
    assertEquals("P001", dataset.rows[0][0])
    assertEquals("28", dataset.rows[0][1])
    assertEquals(com.example.model.VariableType.NUMERIC, dataset.variables[1].type)
  }
}
