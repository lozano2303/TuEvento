import { useState, useEffect } from 'react';
import { MapContainer, TileLayer, Marker, useMapEvents, useMap } from 'react-leaflet';
import { Search, Loader2 } from 'lucide-react';
import L from 'leaflet';
import markerIcon2x from 'leaflet/dist/images/marker-icon-2x.png';
import markerIcon   from 'leaflet/dist/images/marker-icon.png';
import markerShadow from 'leaflet/dist/images/marker-shadow.png';
import { searchAddress } from '../../services/GeocodingService';

// Fix para íconos de marcador rotos en Vite
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: markerIcon2x,
  iconUrl:       markerIcon,
  shadowUrl:     markerShadow,
});

/** Captura clicks en el mapa y hace flyTo cuando cambia el target */
function MapController({ flyTarget }) {
  const map = useMap();
  useEffect(() => {
    if (flyTarget) map.flyTo([flyTarget.lat, flyTarget.lng], 17, { duration: 1 });
  }, [flyTarget, map]);
  return null;
}

/** Captura clicks del usuario en el mapa */
function LocationMarker({ position, onSelect }) {
  useMapEvents({
    click(e) { onSelect({ lat: e.latlng.lat, lng: e.latlng.lng }); },
  });
  return position ? <Marker position={[position.lat, position.lng]} /> : null;
}

/**
 * Selector de punto en mapa con búsqueda de dirección.
 *
 * Props:
 *   value         — { lat, lng } | null
 *   onChange      — (pos: { lat, lng }, opts?: { skipReverseGeocode: boolean }) => void
 *   defaultCenter — [lat, lng]  (centro inicial, ej. coordenadas de la ciudad)
 *   cityBbox      — [south, north, west, east] para sesgar la búsqueda de dirección
 */
export default function SiteMapPicker({ value, onChange, defaultCenter, cityBbox }) {
  const [searchQuery,  setSearchQuery]  = useState('');
  const [isSearching,  setIsSearching]  = useState(false);
  const [searchError,  setSearchError]  = useState(null);
  const [flyTarget,    setFlyTarget]    = useState(null);

  const inputClass =
    'flex-1 bg-background border border-surfaceAlt rounded-lg px-3 py-2 text-sm text-textPrimary ' +
    'focus:outline-none focus:border-accent transition-colors placeholder:text-textMuted';

  const handleSearch = async () => {
    if (!searchQuery.trim()) return;
    setIsSearching(true);
    setSearchError(null);
    try {
      const result = await searchAddress(searchQuery, cityBbox);
      if (!result) {
        setSearchError('No se encontró esa dirección. Intenta con más detalle o márcala directamente en el mapa.');
        return;
      }
      const pos = { lat: result.lat, lng: result.lng };
      onChange(pos, { skipReverseGeocode: true });
      setFlyTarget(pos);
    } catch {
      setSearchError('Error al buscar la dirección. Intenta de nuevo.');
    } finally {
      setIsSearching(false);
    }
  };

  return (
    <div className="space-y-2">
      {/* Barra de búsqueda */}
      <div className="flex gap-2">
        <input
          type="text"
          className={inputClass}
          placeholder="Buscar dirección (ej. Calle 10 # 5-30)"
          value={searchQuery}
          onChange={e => setSearchQuery(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && (e.preventDefault(), handleSearch())}
        />
        <button
          type="button"
          onClick={handleSearch}
          disabled={isSearching}
          className="w-9 h-9 flex items-center justify-center rounded-lg border border-surfaceAlt
                     text-textMuted hover:text-textPrimary hover:bg-surfaceAlt transition-colors
                     disabled:opacity-40"
          title="Buscar dirección"
        >
          {isSearching
            ? <Loader2 className="w-4 h-4 animate-spin" />
            : <Search className="w-4 h-4" />}
        </button>
      </div>

      {searchError && <p className="text-xs text-red-400">{searchError}</p>}

      {/* Mapa */}
      <div className="rounded-lg overflow-hidden border border-surfaceAlt" style={{ height: '256px' }}>
        <MapContainer
          center={defaultCenter ?? [4.5709, -74.2973]}
          zoom={defaultCenter ? 13 : 6}
          style={{ height: '100%', width: '100%' }}
        >
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          <MapController flyTarget={flyTarget} />
          <LocationMarker
            position={value}
            onSelect={pos => { onChange(pos); setFlyTarget(null); }}
          />
        </MapContainer>
      </div>
    </div>
  );
}
