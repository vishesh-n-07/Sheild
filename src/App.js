import { useState } from "react";
import VictimMode from "./components/VictimMode";
import RescuerMode from "./components/RescuerMode";

function App() {
  const [mode, setMode] = useState("home");

  return (
    <div style={{
      minHeight: "100vh",
      backgroundColor: "#0a0a0a",
      color: "white",
      fontFamily: "sans-serif"
    }}>
      {mode === "home" && (
        <div style={{
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          justifyContent: "center",
          minHeight: "100vh",
          padding: "20px"
        }}>
          <div style={{ fontSize: "60px", marginBottom: "10px" }}>🛡️</div>
          <h1 style={{ fontSize: "36px", fontWeight: "bold", marginBottom: "8px" }}>SHEILD</h1>
          <p style={{ color: "#888", marginBottom: "50px", textAlign: "center" }}>
  Smart Human Emergency Instant Location Device
</p>

          <button
            onClick={() => setMode("victim")}
            style={{
              width: "100%",
              maxWidth: "320px",
              padding: "20px",
              backgroundColor: "#dc2626",
              color: "white",
              border: "none",
              borderRadius: "16px",
              fontSize: "18px",
              fontWeight: "bold",
              cursor: "pointer",
              marginBottom: "16px"
            }}
          >
            🆘 I Need Help (Victim Mode)
          </button>

          <button
            onClick={() => setMode("rescuer")}
            style={{
              width: "100%",
              maxWidth: "320px",
              padding: "20px",
              backgroundColor: "#1d4ed8",
              color: "white",
              border: "none",
              borderRadius: "16px",
              fontSize: "18px",
              fontWeight: "bold",
              cursor: "pointer"
            }}
          >
            🚑 I Can Help (Rescuer Mode)
          </button>
        </div>
      )}

      {mode === "victim" && (
        <VictimMode onBack={() => setMode("home")} />
      )}

      {mode === "rescuer" && (
        <RescuerMode onBack={() => setMode("home")} />
      )}
    </div>
  );
}

export default App;