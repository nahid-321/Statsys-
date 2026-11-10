package com.example.engine

import com.example.model.*
import kotlin.random.Random

object SampleDataGenerator {

    fun createMasterDataset(): Dataset {
        val variables = listOf(
            VariableDefinition(
                id = "v1",
                name = "Patient_ID",
                type = VariableType.STRING,
                width = 8,
                decimals = 0,
                label = "Patient Identifier",
                align = AlignmentType.LEFT,
                measure = MeasureType.NOMINAL,
                role = "Input"
            ),
            VariableDefinition(
                id = "v2",
                name = "Group",
                type = VariableType.STRING,
                width = 8,
                decimals = 0,
                label = "Treatment Group (A=Continuous, B=Bolus)",
                valueLabels = mapOf("A" to "Group A", "B" to "Group B"),
                align = AlignmentType.CENTER,
                measure = MeasureType.NOMINAL,
                role = "Input"
            ),
            VariableDefinition(
                id = "v3",
                name = "HTS_Mode",
                type = VariableType.STRING,
                width = 24,
                decimals = 0,
                label = "Hypertonic Saline Administration Mode",
                align = AlignmentType.LEFT,
                measure = MeasureType.NOMINAL,
                role = "Input"
            ),
            VariableDefinition(
                id = "v4",
                name = "Intubated",
                type = VariableType.STRING,
                width = 8,
                decimals = 0,
                label = "Intubation Status on Admission",
                valueLabels = mapOf("Yes" to "Intubated", "No" to "Not Intubated"),
                align = AlignmentType.CENTER,
                measure = MeasureType.NOMINAL,
                role = "Input"
            ),
            VariableDefinition(
                id = "v5",
                name = "Age_years",
                type = VariableType.NUMERIC,
                width = 8,
                decimals = 0,
                label = "Age in Completed Years",
                align = AlignmentType.RIGHT,
                measure = MeasureType.SCALE,
                role = "Input"
            ),
            VariableDefinition(
                id = "v6",
                name = "Age_Group",
                type = VariableType.STRING,
                width = 12,
                decimals = 0,
                label = "Age Stratification Category",
                align = AlignmentType.CENTER,
                measure = MeasureType.ORDINAL,
                role = "Input"
            ),
            VariableDefinition(
                id = "v7",
                name = "Sex",
                type = VariableType.STRING,
                width = 8,
                decimals = 0,
                label = "Biological Sex",
                valueLabels = mapOf("Male" to "Male", "Female" to "Female"),
                align = AlignmentType.CENTER,
                measure = MeasureType.NOMINAL,
                role = "Input"
            ),
            VariableDefinition(
                id = "v8",
                name = "Cause_of_Injury",
                type = VariableType.STRING,
                width = 24,
                decimals = 0,
                label = "Trauma Etiology",
                align = AlignmentType.LEFT,
                measure = MeasureType.NOMINAL,
                role = "Input"
            ),
            VariableDefinition(
                id = "v9",
                name = "TBI_Severity",
                type = VariableType.STRING,
                width = 16,
                decimals = 0,
                label = "Traumatic Brain Injury Severity Classification",
                align = AlignmentType.LEFT,
                measure = MeasureType.ORDINAL,
                role = "Target"
            ),
            VariableDefinition(
                id = "v10",
                name = "Height_m",
                type = VariableType.NUMERIC,
                width = 8,
                decimals = 2,
                label = "Patient Height in meters",
                align = AlignmentType.RIGHT,
                measure = MeasureType.SCALE,
                role = "Input"
            ),
            VariableDefinition(
                id = "v11",
                name = "Weight_kg",
                type = VariableType.NUMERIC,
                width = 8,
                decimals = 0,
                label = "Patient Weight in kilograms",
                align = AlignmentType.RIGHT,
                measure = MeasureType.SCALE,
                role = "Input"
            ),
            VariableDefinition(
                id = "v12",
                name = "ICP_mmHg",
                type = VariableType.NUMERIC,
                width = 8,
                decimals = 1,
                label = "Initial Intracranial Pressure (mmHg)",
                align = AlignmentType.RIGHT,
                measure = MeasureType.SCALE,
                role = "Target"
            ),
            VariableDefinition(
                id = "v13",
                name = "MAP_mmHg",
                type = VariableType.NUMERIC,
                width = 8,
                decimals = 1,
                label = "Mean Arterial Pressure (mmHg)",
                align = AlignmentType.RIGHT,
                measure = MeasureType.SCALE,
                role = "Input"
            ),
            VariableDefinition(
                id = "v14",
                name = "GCS_Score",
                type = VariableType.NUMERIC,
                width = 8,
                decimals = 0,
                label = "Glasgow Coma Scale",
                align = AlignmentType.RIGHT,
                measure = MeasureType.ORDINAL,
                role = "Target"
            )
        )

        // Exact initial 23 rows from user's screenshots:
        val initialRows = listOf(
            listOf("P001", "A", "Continuous infusion", "Yes", "19", "18-30", "Female", "Road traffic accident", "Severe TBI", "1.55", "53", "22.4", "88.5", "5"),
            listOf("P002", "A", "Continuous infusion", "Yes", "20", "18-30", "Male", "Fall from height", "Severe TBI", "1.53", "57", "21.8", "91.0", "6"),
            listOf("P003", "A", "Continuous infusion", "Yes", "71", ">60", "Female", "Road traffic accident", "Severe TBI", "1.48", "47", "24.1", "85.2", "4"),
            listOf("P004", "A", "Continuous infusion", "Yes", "33", "31-45", "Male", "Fall from height", "Severe TBI", "1.63", "58", "19.5", "94.0", "7"),
            listOf("P005", "A", "Continuous infusion", "Yes", "71", ">60", "Female", "Physical assault/others", "Severe TBI", "1.48", "48", "25.0", "83.0", "4"),
            listOf("P006", "A", "Continuous infusion", "Yes", "50", "46-60", "Male", "Road traffic accident", "Severe TBI", "1.52", "57", "23.2", "89.0", "5"),
            listOf("P007", "A", "Continuous infusion", "Yes", "27", "18-30", "Female", "Road traffic accident", "Severe TBI", "1.52", "50", "20.8", "92.5", "6"),
            listOf("P008", "A", "Continuous infusion", "Yes", "26", "18-30", "Male", "Road traffic accident", "Severe TBI", "1.50", "49", "21.5", "90.0", "6"),
            listOf("P009", "A", "Continuous infusion", "Yes", "45", "31-45", "Female", "Road traffic accident", "Severe TBI", "1.52", "50", "22.0", "87.0", "5"),
            listOf("P010", "A", "Continuous infusion", "Yes", "68", ">60", "Male", "Fall from height", "Severe TBI", "1.54", "52", "23.9", "86.5", "4"),
            listOf("P011", "A", "Continuous infusion", "Yes", "25", "18-30", "Male", "Fall from height", "Moderate TBI", "1.53", "51", "16.4", "96.0", "10"),
            listOf("P012", "A", "Continuous infusion", "Yes", "38", "31-45", "Male", "Road traffic accident", "Severe TBI", "1.61", "57", "22.8", "88.0", "5"),
            listOf("P013", "A", "Continuous infusion", "Yes", "23", "18-30", "Male", "Road traffic accident", "Moderate TBI", "1.55", "53", "15.9", "97.5", "11"),
            listOf("P014", "A", "Continuous infusion", "Yes", "44", "31-45", "Female", "Fall from height", "Severe TBI", "1.53", "51", "24.2", "84.0", "4"),
            listOf("P015", "A", "Continuous infusion", "Yes", "43", "31-45", "Female", "Road traffic accident", "Severe TBI", "1.57", "54", "21.6", "90.0", "6"),
            listOf("P016", "A", "Continuous infusion", "Yes", "69", ">60", "Male", "Road traffic accident", "Severe TBI", "1.54", "52", "25.5", "82.5", "3"),
            listOf("P017", "A", "Continuous infusion", "Yes", "38", "31-45", "Male", "Fall from height", "Severe TBI", "1.58", "54", "20.1", "93.0", "7"),
            listOf("P018", "A", "Continuous infusion", "Yes", "55", "46-60", "Male", "Fall from height", "Severe TBI", "1.52", "51", "22.3", "88.0", "5"),
            listOf("P019", "A", "Continuous infusion", "Yes", "41", "31-45", "Male", "Road traffic accident", "Moderate TBI", "1.54", "52", "17.0", "95.0", "9"),
            listOf("P020", "A", "Continuous infusion", "Yes", "36", "31-45", "Male", "Road traffic accident", "Severe TBI", "1.48", "47", "23.0", "87.0", "5"),
            listOf("P021", "A", "Continuous infusion", "Yes", "39", "31-45", "Male", "Road traffic accident", "Severe TBI", "1.53", "51", "21.9", "89.5", "6"),
            listOf("P022", "A", "Continuous infusion", "Yes", "18", "18-30", "Female", "Road traffic accident", "Moderate TBI", "1.50", "49", "16.8", "96.0", "10"),
            listOf("P023", "A", "Continuous infusion", "Yes", "60", "46-60", "Male", "Road traffic accident", "Severe TBI", "1.53", "51", "23.5", "86.0", "5")
        )

        val fullRows = ArrayList<List<String>>(122)
        fullRows.addAll(initialRows)

        // Generate realistic remaining rows up to 122 cases exactly matching the bottom status bar!
        val rng = Random(12345)
        val causes = listOf("Road traffic accident", "Fall from height", "Physical assault/others")
        val severities = listOf("Severe TBI", "Moderate TBI", "Mild TBI")

        for (i in 24..122) {
            val pid = "P" + i.toString().padStart(3, '0')
            val group = if (i <= 61) "A" else "B"
            val htsMode = if (group == "A") "Continuous infusion" else "Bolus infusion"
            val intubated = if (rng.nextDouble() < 0.85) "Yes" else "No"
            val age = rng.nextInt(18, 76)
            val ageGroup = when {
                age <= 30 -> "18-30"
                age <= 45 -> "31-45"
                age <= 60 -> "46-60"
                else -> ">60"
            }
            val sex = if (rng.nextDouble() < 0.65) "Male" else "Female"
            val cause = causes[rng.nextInt(causes.size)]
            val severity = if (intubated == "Yes") {
                if (rng.nextDouble() < 0.75) "Severe TBI" else "Moderate TBI"
            } else {
                severities[rng.nextInt(severities.size)]
            }
            val height = 1.48 + rng.nextDouble() * 0.22
            val weight = (height * height * (20 + rng.nextDouble() * 7)).toInt()
            val icp = if (group == "A") 16.0 + rng.nextDouble() * 8.0 else 18.0 + rng.nextDouble() * 9.0
            val map = 80.0 + rng.nextDouble() * 20.0
            val gcs = if (severity == "Severe TBI") rng.nextInt(3, 9) else if (severity == "Moderate TBI") rng.nextInt(9, 13) else rng.nextInt(13, 16)

            fullRows.add(
                listOf(
                    pid,
                    group,
                    htsMode,
                    intubated,
                    age.toString(),
                    ageGroup,
                    sex,
                    cause,
                    severity,
                    String.format("%.2f", height),
                    weight.toString(),
                    String.format("%.1f", icp),
                    String.format("%.1f", map),
                    gcs.toString()
                )
            )
        }

        return Dataset(
            name = "Master data corrected 2.xlsx",
            variables = variables,
            rows = fullRows
        )
    }
}
