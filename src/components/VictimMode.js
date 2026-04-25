import { useState, useRef } from "react";
import { supabase } from "../supabase";

function VictimMode({ onBack }) {
  const [status, setStatus] = useState("idle");
  const [location, setLocation] = useState(null);
  const [audioURL, setAudioURL] = useState(null);
  const [name, setName] = useState("");
  const mediaRecorderRef = useRef(null);
  const audioChunksRef = useRef([]);
  const locationRef = useRef(null);

  const triggerSOS = async () => {
    if (!name) {
      alert("Please enter your name first!");
      return;
    }

    setStatus("recording");

    // Get location
    navigator.geolocation.getCurrentPosition((pos) => {
      const loc = {
        lat: pos.coords.latitude,
        lng: pos.coords.longitude
      };
      setLocation(loc);
      locationRef.current = loc;
    });

    // Start recording
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
    const mediaRecorder = new MediaRecorder(stream);
    mediaRecorderRef.current = mediaRecorder;
    audioChunksRef.current = [];

    mediaRecorder.ondataavailable = (e) => {
      audioChunksRef.current.push(e.data);
    };

    mediaRecorder.onstop = async () => {
      const audioBlob = new Blob(audioChunksRef.current, { type: "audio/webm" });
      const url = URL.createObjectURL(audioBlob);
      setAudioURL(url);

      // Upload audio to Supabase Storage
      const fileName = `sos_${Date.now()}.webm`;
      const { data: uploadData, error: uploadError } = await supabase.storage
        .from("audio")
        .upload(fileName, audioBlob, {
          contentType: "audio/webm"
        });

      let audioPublicUrl = null;
      if (!uploadError) {
        const { data: urlData } = supabase.storage
          .from("audio")
          .getPublicUrl(fileName);
        audioPublicUrl = urlData.publicUrl;
        console.log("Audio uploaded:", audioPublicUrl);
      } else {
        console.error("Audio upload error:", uploadError);
      }

      // Send real alert to Supabase
      const { error } = await supabase
        .from("alerts")
        .insert([{
          victim_name: name,
          lat: locationRef.current?.lat || 0,
          lng: locationRef.current?.lng || 0,
          timestamp: Date.now(),
          audio_url: audioPublicUrl
        }]);

      if (error) {
        console.error("Error sending alert:", error);
      } else {
        console.log("Alert sent to Supabase!");
      }

      setStatus("sent");
    };

    mediaRecorder.start();

    setTimeout(() => {
      if (mediaRecorderRef.current?.state === "recording") {
        mediaRecorderRef.current.stop();
      }
    }, 60000);
  };

  const stopRecording = () => {
    if (mediaRecorderRef.current?.state === "recording") {
      mediaRecorderRef.current.stop();
    }
  };

  return (
    <div style={{
      minHeight: "100vh",
      backgroundColor: "#0a0a0a",
      padding: "20px",
      display: "flex",
      flexDirection: "column",
      alignItems: "center"
    }}>
      <div style={{ width: "100%", maxWidth: "400px" }}>
        <button onClick={onBack} style={{
          background: "none",
          border: "none",
          color: "#888",
          fontSize: "16px",
          cursor: "pointer",
          marginBottom: "20px"
        }}>← Back</button>

        <h2 style={{ fontSize: "24px", fontWeight: "bold", marginBottom: "4px" }}>
          🆘 Victim Mode
        </h2>
        <p style={{ color: "#888", marginBottom: "30px" }}>
          Press SOS to alert nearby rescuers
        </p>

        {status === "idle" && (
          <input
            type="text"
            placeholder="Enter your name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            style={{
              width: "100%",
              padding: "14px",
              backgroundColor: "#1a1a1a",
              border: "1px solid #333",
              borderRadius: "12px",
              color: "white",
              fontSize: "16px",
              marginBottom: "30px",
              boxSizing: "border-box"
            }}
          />
        )}

        {status === "idle" && (
          <div style={{ textAlign: "center" }}>
            <button
              onClick={triggerSOS}
              style={{
                width: "200px",
                height: "200px",
                borderRadius: "50%",
                backgroundColor: "#dc2626",
                color: "white",
                border: "none",
                fontSize: "24px",
                fontWeight: "bold",
                cursor: "pointer",
                boxShadow: "0 0 40px rgba(220, 38, 38, 0.5)"
              }}
            >
              🆘 SOS
            </button>
            <p style={{ color: "#666", marginTop: "20px", fontSize: "14px" }}>
              Press to send emergency alert
            </p>
          </div>
        )}

        {status === "recording" && (
          <div style={{ textAlign: "center" }}>
            <div style={{
              width: "200px",
              height: "200px",
              borderRadius: "50%",
              backgroundColor: "#7f1d1d",
              display: "flex",
              flexDirection: "column",
              alignItems: "center",
              justifyContent: "center",
              margin: "0 auto"
            }}>
              <div style={{ fontSize: "40px" }}>🎙️</div>
              <p style={{ color: "white", fontWeight: "bold" }}>Recording...</p>
            </div>

            <p style={{ color: "#ef4444", marginTop: "20px" }}>
              🚨 Alert sent! Recording evidence...
            </p>

            {location && (
              <p style={{ color: "#22c55e", marginTop: "10px", fontSize: "14px" }}>
                📍 Location captured: {location.lat.toFixed(4)}, {location.lng.toFixed(4)}
              </p>
            )}

            <button
              onClick={stopRecording}
              style={{
                marginTop: "20px",
                padding: "12px 24px",
                backgroundColor: "#333",
                color: "white",
                border: "none",
                borderRadius: "12px",
                cursor: "pointer"
              }}
            >
              Stop Recording
            </button>
          </div>
        )}

        {status === "sent" && (
          <div style={{ textAlign: "center" }}>
            <div style={{ fontSize: "80px", marginBottom: "20px" }}>✅</div>
            <h3 style={{ color: "#22c55e", fontSize: "24px" }}>Alert Sent!</h3>
            <p style={{ color: "#888", marginTop: "10px" }}>
              Nearby rescuers have been notified
            </p>

            {location && (
              <div style={{
                backgroundColor: "#1a1a1a",
                borderRadius: "12px",
                padding: "16px",
                marginTop: "20px",
                textAlign: "left"
              }}>
                <p style={{ color: "#22c55e", fontSize: "14px" }}>
                  📍 {location.lat.toFixed(4)}, {location.lng.toFixed(4)}
                </p>
                <p style={{ color: "#888", fontSize: "12px", marginTop: "4px" }}>
                  Location shared with rescuers
                </p>
              </div>
            )}

            {audioURL && (
              <div style={{
                backgroundColor: "#1a1a1a",
                borderRadius: "12px",
                padding: "16px",
                marginTop: "12px"
              }}>
                <p style={{ color: "#888", fontSize: "14px", marginBottom: "8px" }}>
                  🎙️ Voice evidence recorded
                </p>
                <audio controls src={audioURL} style={{ width: "100%" }} />
              </div>
            )}

            <button
              onClick={() => setStatus("idle")}
              style={{
                marginTop: "20px",
                padding: "12px 24px",
                backgroundColor: "#333",
                color: "white",
                border: "none",
                borderRadius: "12px",
                cursor: "pointer"
              }}
            >
              Send Another Alert
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

export default VictimMode;