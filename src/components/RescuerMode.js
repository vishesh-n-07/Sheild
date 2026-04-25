import { useState, useEffect } from "react";
import { supabase } from "../supabase";

function timeAgo(timestamp) {
  const seconds = Math.floor((Date.now() - timestamp) / 1000);
  if (seconds < 60) return `${seconds}s ago`;
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m ago`;
  return `${Math.floor(minutes / 60)}h ago`;
}

function getDistance(lat1, lng1, lat2, lng2) {
  const R = 6371;
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLng = (lng2 - lng1) * Math.PI / 180;
  const a = Math.sin(dLat/2) * Math.sin(dLat/2) +
    Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
    Math.sin(dLng/2) * Math.sin(dLng/2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
  return R * c;
}

function RescuerMode({ onBack }) {
  const [alerts, setAlerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [rescuerLocation, setRescuerLocation] = useState(null);
  const [responding, setResponding] = useState(null);

  useEffect(() => {
    navigator.geolocation.getCurrentPosition((pos) => {
      setRescuerLocation({
        lat: pos.coords.latitude,
        lng: pos.coords.longitude
      });
    });

    fetchAlerts();

    const subscription = supabase
      .channel("alerts")
      .on("postgres_changes", {
        event: "INSERT",
        schema: "public",
        table: "alerts"
      }, (payload) => {
        setAlerts(prev => [payload.new, ...prev]);
      })
      .subscribe();

    return () => {
      supabase.removeChannel(subscription);
    };
  }, []);

  const fetchAlerts = async () => {
    const twentyFourHoursAgo = Date.now() - 86400000;

    const { data, error } = await supabase
      .from("alerts")
      .select("*")
      .gte("timestamp", twentyFourHoursAgo)
      .order("timestamp", { ascending: false });

    if (error) {
      console.error("Error fetching alerts:", error);
    } else {
      setAlerts(data);
    }
    setLoading(false);
  };

  const getFilteredAlerts = () => {
    if (!rescuerLocation) return alerts;
    return alerts.filter(alert => {
      const distance = getDistance(
        rescuerLocation.lat, rescuerLocation.lng,
        alert.lat, alert.lng
      );
      return distance <= 2;
    });
  };

  const openMaps = (lat, lng) => {
    window.open(`https://www.google.com/maps?q=${lat},${lng}`, "_blank");
  };

  const respondToAlert = (id) => {
    setResponding(id);
    setTimeout(() => {
      setAlerts(prev => prev.filter(a => a.id !== id));
      setResponding(null);
    }, 1000);
  };

  const filteredAlerts = getFilteredAlerts();

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
          🚑 Rescuer Mode
        </h2>
        <p style={{ color: "#888", marginBottom: "20px" }}>
          Live alerts within 2km radius
        </p>

        <div style={{
          display: "flex",
          alignItems: "center",
          gap: "8px",
          backgroundColor: "#1a1a1a",
          padding: "10px 16px",
          borderRadius: "12px",
          marginBottom: "20px"
        }}>
          <div style={{
            width: "10px",
            height: "10px",
            borderRadius: "50%",
            backgroundColor: "#22c55e",
          }} />
          <span style={{ color: "#22c55e", fontSize: "14px" }}>
            Listening for nearby alerts...
          </span>
        </div>

        {loading && (
          <div style={{ textAlign: "center", padding: "40px" }}>
            <p style={{ color: "#888" }}>Scanning for nearby alerts...</p>
          </div>
        )}

        {!loading && filteredAlerts.length === 0 && (
          <div style={{
            textAlign: "center",
            padding: "40px",
            backgroundColor: "#1a1a1a",
            borderRadius: "16px"
          }}>
            <div style={{ fontSize: "40px", marginBottom: "12px" }}>✅</div>
            <p style={{ color: "#888" }}>No active alerts nearby</p>
            <p style={{ color: "#555", fontSize: "14px" }}>Area is safe</p>
          </div>
        )}

        {!loading && filteredAlerts.map(alert => {
          const distance = rescuerLocation
            ? getDistance(rescuerLocation.lat, rescuerLocation.lng, alert.lat, alert.lng).toFixed(1)
            : "?";

          return (
            <div key={alert.id} style={{
              backgroundColor: "#1a1a1a",
              borderRadius: "16px",
              padding: "16px",
              marginBottom: "16px",
              borderLeft: "4px solid #dc2626"
            }}>
              <div style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
                marginBottom: "12px"
              }}>
                <div>
                  <p style={{ fontWeight: "bold", fontSize: "16px" }}>
                    🆘 {alert.victim_name}
                  </p>
                  <p style={{ color: "#888", fontSize: "12px" }}>
                    {timeAgo(alert.timestamp)}
                  </p>
                </div>
                <span style={{
                  backgroundColor: "#7f1d1d",
                  color: "#fca5a5",
                  padding: "4px 10px",
                  borderRadius: "20px",
                  fontSize: "12px"
                }}>
                  {distance} km
                </span>
              </div>

              <p style={{ color: "#888", fontSize: "13px", marginBottom: "12px" }}>
                📍 {alert.lat?.toFixed(4)}, {alert.lng?.toFixed(4)}
              </p>

              {alert.audio_url && (
                <div style={{ marginBottom: "12px" }}>
                  <p style={{ color: "#888", fontSize: "12px", marginBottom: "6px" }}>
                    🎙️ Voice evidence:
                  </p>
                  <audio controls src={alert.audio_url} style={{ width: "100%" }} />
                </div>
              )}

              <div style={{ display: "flex", gap: "10px" }}>
                <button
                  onClick={() => openMaps(alert.lat, alert.lng)}
                  style={{
                    flex: 1,
                    padding: "10px",
                    backgroundColor: "#1d4ed8",
                    color: "white",
                    border: "none",
                    borderRadius: "10px",
                    cursor: "pointer",
                    fontSize: "14px"
                  }}
                >
                  🗺️ Navigate
                </button>
                <button
                  onClick={() => respondToAlert(alert.id)}
                  style={{
                    flex: 1,
                    padding: "10px",
                    backgroundColor: responding === alert.id ? "#333" : "#16a34a",
                    color: "white",
                    border: "none",
                    borderRadius: "10px",
                    cursor: "pointer",
                    fontSize: "14px"
                  }}
                >
                  {responding === alert.id ? "..." : "✅ Responding"}
                </button>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

export default RescuerMode;