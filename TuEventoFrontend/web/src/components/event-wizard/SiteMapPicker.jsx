import { MapContainer, TileLayer, Marker, useMapEvents } from 'react-leaflet';
import L from 'leaflet';
import markerIcon2x from 'leaflet/dist/images/marker-icon-2x.png';
import markerIcon   from 'leaflet/dist/images/marker-icon.png';
import markerShadow from 'leaflet/dist/images/marker-shadow.png';

// Fix para íconos de marcador en Vite/webpack (rutas rotas del bundle de Leaflet)
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: markerIcon2x,
  iconUrl:       markerIcon,
  shadowUrl:     markerShadow,
});

/** Componente interno que captura clicks en el mapa */
function LocationMarker({ position, onSelect }) {
  useMapEvents({
    click(e) {
      onSelect({ lat: e.latlng.lat, lng: e.latlng.lng });
    },
  });
  return position ? <Marker position={[position.lat, position.lng]} /> : null;
}

/**
 * Selector de punto en mapa con Leaflet + OpenStreetMap.
 *
 * Props:
 *   value         — { lat, lng } | null — punto actualmente seleccionado
 *   onChange      — (loc: { lat, lng }) => void
 *   defaultCenter — [lat, lng] — centro inicial del mapa (default: Colombia)
 */
export default function SiteMapPicker({ value, onChange, defaultCenter }) {
  return (
    <div className="rounded-lg overflow-hidden border border-surfaceAlt" style={{ height: '256px' }}>
      <MapContainer
        center={defaultCenter ?? [4.5709, -74.2973]}
        zoom={value ? 15 : 6}
        style={{ height: '100%', width: '100%' }}
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <LocationMarker position={value} onSelect={onChange} />
      </MapContainer>
    </div>
  );
}
