package me.geetprince.Eventify.util;

import me.geetprince.Eventify.model.Registration;

import java.util.List;

public class CSVExportUtil {
    public static String generateRegistrationCSV(List<Registration> registrations) {
        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append("Name,Email,Roll Number,Department,Study Year,Checked In,Registration Date\n");
        for (Registration reg : registrations) {
            csvBuilder.append(escapeSpecialCharacters(reg.getAttendeeName())).append(",")
                    .append(escapeSpecialCharacters(reg.getAttendeeEmail())).append(",")
                    .append(escapeSpecialCharacters(reg.getRollNumber())).append(",")
                    .append(escapeSpecialCharacters(reg.getDepartment())).append(",")
                    .append(reg.getStudyYear() != null ? reg.getStudyYear() : "").append(",")
                    .append(reg.getCheckedIn() ? "Yes" : "No").append(",")
                    .append(reg.getRegisteredAt() != null ? reg.getRegisteredAt().toString() : "")
                    .append("\n");
        }

        return csvBuilder.toString();
    }
    private static String escapeSpecialCharacters(String data) {
        if (data == null) {
            return "";
        }
        String escapedData = data.replaceAll("\\R", " ");
        if (data.contains(",") || data.contains("\"") || data.contains("'")) {
            data = data.replace("\"", "\"\"");
            escapedData = "\"" + data + "\"";
        }
        return escapedData;
    }
}
