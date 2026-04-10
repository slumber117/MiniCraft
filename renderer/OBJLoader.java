package minicraft.renderer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import minicraft.math.Vector3f;
import minicraft.math.Vector2f;

public class OBJLoader {

    public static Mesh loadModel(String fileName, Texture texture) throws Exception {
        List<Vector3f> vertices = new ArrayList<>();
        List<Vector2f> textures = new ArrayList<>();
        List<IndexGroup> faces = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(OBJLoader.class.getResourceAsStream(fileName)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.trim().split("\\s+");
                if (tokens.length < 1) continue;
                
                switch (tokens[0]) {
                    case "v":
                        vertices.add(new Vector3f(Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2]), Float.parseFloat(tokens[3])));
                        break;
                    case "vt":
                        textures.add(new Vector2f(Float.parseFloat(tokens[1]), 1.0f - Float.parseFloat(tokens[2])));
                        break;
                    case "f":
                        for (int i = 1; i <= Math.min(3, tokens.length - 1); i++) {
                            faces.add(parseFace(tokens[i]));
                        }
                        break;
                }
            }
        }

        return reorderBuffers(vertices, textures, faces, texture);
    }

    private static IndexGroup parseFace(String token) {
        String[] tokens = token.split("/");
        IndexGroup group = new IndexGroup();
        group.idxPos = Integer.parseInt(tokens[0]) - 1;
        if (tokens.length > 1 && !tokens[1].isEmpty()) {
            group.idxTextCoord = Integer.parseInt(tokens[1]) - 1;
        }
        return group;
    }

    private static Mesh reorderBuffers(List<Vector3f> posList, List<Vector2f> textCoordList, List<IndexGroup> faces, Texture texture) {
        float[] posArr = new float[faces.size() * 3];
        float[] textCoordArr = new float[faces.size() * 2];
        int[] indicesArr = new int[faces.size()];

        for (int i = 0; i < faces.size(); i++) {
            IndexGroup face = faces.get(i);
            Vector3f pos = posList.get(face.idxPos);
            posArr[i * 3] = pos.x; posArr[i * 3 + 1] = pos.y; posArr[i * 3 + 2] = pos.z;

            if (face.idxTextCoord >= 0 && face.idxTextCoord < textCoordList.size()) {
                Vector2f textCoord = textCoordList.get(face.idxTextCoord);
                textCoordArr[i * 2] = textCoord.x; textCoordArr[i * 2 + 1] = textCoord.y;
            }
            indicesArr[i] = i;
        }
        return new Mesh(posArr, textCoordArr, indicesArr, texture);
    }

    private static class IndexGroup { public int idxPos = -1; public int idxTextCoord = -1; }
}
