import { defineConfig } from "vite";
import scalaJSPlugin from "@scala-js/vite-plugin-scalajs";

// the Scala.js output is imported as "scalajs:main.js"; the data bundle is served from public/data/<version>/
export default defineConfig({
  plugins: [scalaJSPlugin({ cwd: ".", projectID: "atlas" })],
});
