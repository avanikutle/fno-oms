package com.fnooms.util;

import java.io.IOException;

public class SSTest {
        public static void main(String[] args) throws IOException, InterruptedException {

                System.out.println("Hello motto" + CredsUtil.getWsCreds("mstock"));

                // HttpClient client = HttpClient.newBuilder()
                // .followRedirects(HttpClient.Redirect.NORMAL)
                // .build();

                // HttpRequest request = HttpRequest.newBuilder()
                // .uri(URI.create("https://api.mstock.trade/openapi/typea/portfolio/holdings"))
                // .GET()
                // .setHeader("X-Mirae-Version", "1")
                // .setHeader("Authorization",
                // "token
                // Yl9xwVVZSDBXwpozaReFWw==:eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJVU0VSTkFNRSI6Ik1BNjg3NiIsIkNMSUVOVE5BTUUiOiJTVURIRUVSIFJFRERZIExFTktBTEEiLCJVU0VSX0RFVEFJTFMiOiJjUWo5V2F6dGFxM25TaUMrZW14Z3VDaXpxcTV0Vkh6RGtiZU5sTHFseFVnVWNVZDQzMzBjSVNUTVpyN3V3aEgzUGkvREhyRUYyZlMwS01XVU5LSXRKVXYxYXYrb2ZtNUFteFBnVGJBaStkWFVIQVU3NjVyb1NxMi9qZm5rbUI2c1ZuWUZuTTVzOTBoYlR4akJtSkg5N2JQWUdVc2Roa2F6RGF6U3QwbXJKVDJ1SlU3U1pzQWxTV3lxUVV0SXZHalFXMURpVHUwM1lTNlA2aUVBT1U2SU5MaERvZzNIbzdyTGlpUDZ6eFJNeWRtMEdibkVWbHFrWmhrcXducHRhSmJ2NEx2NXVNa0U2NzE2MWNhUVNvNWtkKzUzdXBWaEV6UkVSL2dWTi9ZeGpqQks2anorZisvdUFNUTJWakhzNUdKUFMxRHdBd2dLSFhZNWd5VzRDVUlHbUE9PSIsIlVTRVJJRCI6Ik1BNjg3NiIsIkFDQ0VTU19UT0tFTiI6ImV5SmhiR2NpT2lKSVV6STFOaUlzSW5SNWNDSTZJa3BYVkNKOS5leUpoZFdRaU9pSnRhWEpoWlM1cGJpSXNJbVY0Y0NJNk1UYzRNemt6TWpBeU1Dd2lhV0YwSWpveE56Z3pPRFExTmpJd0xDSnBjM01pT2lKdGFYSmhaUzVwYmlJc0ltNWlaaUk2TVRRME5EUTNPRFF3TUN3aWNHWnRJam9pTVNJc0luUnBaQ0k2SWpReklpd2lkV2xrSWpvaU5EZzFOemt3SWl3aWRtbGtJam9pTWpFaWZRLmNWMkFXaTNseTdTS0VHdTkwYXJJbi1ZTXRZV0ZRZ2V1bEd0VTRlWWQtUW8iLCJBUElUWVBFIjoiVFlQRUEiLCJVSUQiOiJjMTM4ODljNi1kMmJkLTQwNDUtYWRmYy05Yzg0MDgxMzUwYWIiLCJuYmYiOjE3ODM4NDU2MjAsImV4cCI6MTc4Mzg4MTAwMCwiaWF0IjoxNzgzODQ1NjIwfQ.j4Vz0mGh3RA6z5Gz7kV1LVcXpAcM3oLo2rEbJyCjwe0")
                // .build();

                // HttpResponse<String> response = client.send(request,
                // HttpResponse.BodyHandlers.ofString());
                // System.out.println(response.body());
        }
}
