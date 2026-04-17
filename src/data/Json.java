package data;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Json {
    private final String nomeArquivo;
    private List<Map<String, Object>> leiloes;

    public Json(String nomeArquivo) {
        this.nomeArquivo = nomeArquivo;
        this.leiloes = new ArrayList<>();
        carregar();
    }

    private void carregar() {
        try {
            File arquivo = new File(nomeArquivo);
            if (arquivo.exists()) {
                String conteudo = new String(Files.readAllBytes(arquivo.toPath()));
                if (!conteudo.trim().isEmpty()) {
                    leiloes = parseJson(conteudo);
                }
            }
        } catch (IOException e) {
            leiloes = new ArrayList<>();
        }
    }

    public void adicionarLeilao(Map<String, Object> leilao) {
        leiloes.add(leilao);
    }

    public void salvar() throws IOException {
        String json = toJson(leiloes);
        Files.write(Paths.get(nomeArquivo), json.getBytes());
    }

    private String toJson(List<Map<String, Object>> lista) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");

        for (int i = 0; i < lista.size(); i++) {
            Map<String, Object> obj = lista.get(i);
            sb.append("  {\n");
            int count = 0;
            int size = obj.size();
            for (Map.Entry<String, Object> entry : obj.entrySet()) {
                sb.append("    \"").append(entry.getKey()).append("\": ");
                Object value = entry.getValue();
                if (value instanceof Number) {
                    sb.append(value);
                } else if (value instanceof List) {
                    sb.append("[");
                    List<?> list = (List<?>) value;
                    for (int j = 0; j < list.size(); j++) {
                        sb.append("\"").append(list.get(j)).append("\"");
                        if (j < list.size() - 1) sb.append(", ");
                    }
                    sb.append("]");
                } else {
                    sb.append("\"").append(value).append("\"");
                }
                count++;
                if (count < size) sb.append(",\n");
                else sb.append("\n");
            }
            sb.append("  }");
            if (i < lista.size() - 1) sb.append(",");
            sb.append("\n");
        }

        sb.append("]");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseJson(String json) {
        List<Map<String, Object>> resultado = new ArrayList<Map<String, Object>>();
        json = json.trim();

        if (!json.startsWith("[")) return resultado;

        json = json.substring(1, json.length() - 1).trim();
        if (json.isEmpty()) return resultado;

        int nivel = 0;
        int inicioObj = -1;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') {
                if (nivel == 0) inicioObj = i;
                nivel++;
            } else if (c == '}') {
                nivel--;
                if (nivel == 0 && inicioObj != -1) {
                    String objStr = json.substring(inicioObj, i + 1);
                    Map<String, Object> mapa = parseObjeto(objStr);
                    resultado.add(mapa);
                    inicioObj = -1;
                }
            }
        }

        return resultado;
    }

    private Map<String, Object> parseObjeto(String objStr) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        objStr = objStr.trim();

        if (!objStr.startsWith("{") || !objStr.endsWith("}")) return mapa;
        objStr = objStr.substring(1, objStr.length() - 1).trim();

        int pos = 0;
        while (pos < objStr.length()) {
            int aspasIni = objStr.indexOf('"', pos);
            if (aspasIni == -1) break;

            int chaveFim = objStr.indexOf('"', aspasIni + 1);
            String chave = objStr.substring(aspasIni + 1, chaveFim);

            int doisPontos = objStr.indexOf(':', chaveFim);
            if (doisPontos == -1) break;

            pos = doisPontos + 1;
            while (pos < objStr.length() && Character.isWhitespace(objStr.charAt(pos))) pos++;

            Object valor = null;
            if (objStr.charAt(pos) == '"') {
                int fim = objStr.indexOf('"', pos + 1);
                valor = objStr.substring(pos + 1, fim);
                pos = fim + 1;
            } else if (objStr.charAt(pos) == '[') {
                int fim = objStr.indexOf(']', pos);
                String listaStr = objStr.substring(pos + 1, fim);
                List<String> lista = new ArrayList<>();
                if (!listaStr.trim().isEmpty()) {
                    String[] itens = listaStr.split(",");
                    for (String item : itens) {
                        item = item.trim();
                        if (item.startsWith("\"")) item = item.substring(1);
                        if (item.endsWith("\"")) item = item.substring(0, item.length() - 1);
                        lista.add(item);
                    }
                }
                valor = lista;
                pos = fim + 1;
            } else if (Character.isDigit(objStr.charAt(pos))) {
                StringBuilder num = new StringBuilder();
                while (pos < objStr.length() && (Character.isDigit(objStr.charAt(pos)) || objStr.charAt(pos) == '.')) {
                    num.append(objStr.charAt(pos));
                    pos++;
                }
                if (num.toString().contains(".")) {
                    valor = Double.parseDouble(num.toString());
                } else {
                    valor = Integer.parseInt(num.toString());
                }
            }

            mapa.put(chave, valor);
            while (pos < objStr.length() && (objStr.charAt(pos) == ',' || Character.isWhitespace(objStr.charAt(pos)))) pos++;
        }

        return mapa;
    }
}