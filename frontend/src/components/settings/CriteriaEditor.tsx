import { useState, useEffect } from 'react';
import {
  Box,
  Button,
  Chip,
  FormControlLabel,
  Slider,
  Switch,
  TextField,
  Typography,
} from '@mui/material';
import { GlassCard } from '../common/GlassCard';
import type { Criteria, CriteriaCreate, CriteriaUpdate } from '../../types';

interface CriteriaEditorProps {
  criteria?: Criteria | null;
  onSave: (data: CriteriaCreate | CriteriaUpdate) => Promise<void>;
  onCancel: () => void;
  isSaving?: boolean;
}

export const CriteriaEditor = ({ criteria, onSave, onCancel, isSaving }: CriteriaEditorProps) => {
  const [name, setName] = useState('');
  const [capabilities, setCapabilities] = useState<string[]>([]);
  const [sectors, setSectors] = useState<string[]>([]);
  const [locations, setLocations] = useState<string[]>([]);
  const [keywords, setKeywords] = useState<string[]>([]);
  const [valueRange, setValueRange] = useState<[number, number]>([0, 10000000]);
  const [isDefault, setIsDefault] = useState(false);

  const [capInput, setCapInput] = useState('');
  const [sectorInput, setSectorInput] = useState('');
  const [locationInput, setLocationInput] = useState('');
  const [keywordInput, setKeywordInput] = useState('');

  useEffect(() => {
    if (criteria) {
      setName(criteria.name);
      setCapabilities(criteria.capabilities);
      setSectors(criteria.preferred_sectors);
      setLocations(criteria.preferred_locations);
      setKeywords(criteria.keywords);
      setValueRange([criteria.value_range.min, criteria.value_range.max ?? 10000000]);
      setIsDefault(criteria.is_default);
    }
  }, [criteria]);

  const handleAddTag = (
    value: string,
    setter: React.Dispatch<React.SetStateAction<string[]>>,
    inputSetter: React.Dispatch<React.SetStateAction<string>>
  ) => {
    if (value.trim()) {
      setter((prev) => [...prev, value.trim()]);
      inputSetter('');
    }
  };

  const handleRemoveTag = (
    index: number,
    setter: React.Dispatch<React.SetStateAction<string[]>>
  ) => {
    setter((prev) => prev.filter((_, i) => i !== index));
  };

  const handleSave = async () => {
    const data: CriteriaCreate = {
      name,
      capabilities,
      preferred_sectors: sectors,
      preferred_locations: locations,
      keywords,
      value_range: {
        min: valueRange[0],
        max: valueRange[1] >= 10000000 ? null : valueRange[1],
      },
      is_default: isDefault,
    };
    await onSave(data);
  };

  const TagInput = ({
    label,
    value,
    tags,
    onChange,
    onAdd,
    onRemove,
    chipColor,
  }: {
    label: string;
    value: string;
    tags: string[];
    onChange: (v: string) => void;
    onAdd: () => void;
    onRemove: (i: number) => void;
    chipColor: string;
  }) => (
    <Box>
      <Typography variant="caption" sx={{ color: 'text.secondary', display: 'block', mb: 0.5 }}>
        {label}
      </Typography>
      <Box sx={{ display: 'flex', gap: 1, mb: 1 }}>
        <TextField
          size="small"
          value={value}
          onChange={(e) => onChange(e.target.value)}
          onKeyPress={(e) => e.key === 'Enter' && (e.preventDefault(), onAdd())}
          placeholder={`Add ${label.toLowerCase()}`}
          sx={{ flex: 1 }}
        />
        <Button variant="outlined" size="small" onClick={onAdd}>
          Add
        </Button>
      </Box>
      <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap' }}>
        {tags.map((tag, i) => (
          <Chip
            key={i}
            label={tag}
            size="small"
            onDelete={() => onRemove(i)}
            sx={{ backgroundColor: `${chipColor}20`, color: chipColor }}
          />
        ))}
      </Box>
    </Box>
  );

  return (
    <GlassCard>
      <Typography variant="h6" sx={{ mb: 3, fontWeight: 600 }}>
        {criteria ? 'Edit Criteria' : 'New Criteria'}
      </Typography>

      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
        <TextField
          label="Name"
          value={name}
          onChange={(e) => setName(e.target.value)}
          fullWidth
          required
        />

        <TagInput
          label="Capabilities"
          value={capInput}
          tags={capabilities}
          onChange={setCapInput}
          onAdd={() => handleAddTag(capInput, setCapabilities, setCapInput)}
          onRemove={(i) => handleRemoveTag(i, setCapabilities)}
          chipColor="#8B5CF6"
        />

        <TagInput
          label="Sectors"
          value={sectorInput}
          tags={sectors}
          onChange={setSectorInput}
          onAdd={() => handleAddTag(sectorInput, setSectors, setSectorInput)}
          onRemove={(i) => handleRemoveTag(i, setSectors)}
          chipColor="#EC4899"
        />

        <TagInput
          label="Locations"
          value={locationInput}
          tags={locations}
          onChange={setLocationInput}
          onAdd={() => handleAddTag(locationInput, setLocations, setLocationInput)}
          onRemove={(i) => handleRemoveTag(i, setLocations)}
          chipColor="#3B82F6"
        />

        <TagInput
          label="Keywords"
          value={keywordInput}
          tags={keywords}
          onChange={setKeywordInput}
          onAdd={() => handleAddTag(keywordInput, setKeywords, setKeywordInput)}
          onRemove={(i) => handleRemoveTag(i, setKeywords)}
          chipColor="#64748B"
        />

        <Box>
          <Typography variant="caption" sx={{ color: 'text.secondary', display: 'block', mb: 1 }}>
            Value Range (ZAR)
          </Typography>
          <Slider
            value={valueRange}
            onChange={(_, v) => setValueRange(v as [number, number])}
            min={0}
            max={10000000}
            step={100000}
            valueLabelDisplay="auto"
            valueLabelFormat={(v) =>
              v >= 10000000 ? 'Any' : `R ${(v / 1000000).toFixed(1)}M`
            }
            sx={{
              '& .MuiSlider-thumb': {
                background: 'linear-gradient(135deg, #8B5CF6 0%, #EC4899 100%)',
              },
              '& .MuiSlider-track': {
                background: 'linear-gradient(90deg, #8B5CF6 0%, #EC4899 100%)',
              },
            }}
          />
          <Typography variant="caption" sx={{ color: 'text.secondary' }}>
            R {valueRange[0].toLocaleString()} -{' '}
            {valueRange[1] >= 10000000 ? 'Any' : `R ${valueRange[1].toLocaleString()}`}
          </Typography>
        </Box>

        <FormControlLabel
          control={
            <Switch
              checked={isDefault}
              onChange={(e) => setIsDefault(e.target.checked)}
              color="primary"
            />
          }
          label="Set as default criteria"
        />

        <Box sx={{ display: 'flex', gap: 2, justifyContent: 'flex-end' }}>
          <Button variant="outlined" onClick={onCancel}>
            Cancel
          </Button>
          <Button
            variant="contained"
            onClick={handleSave}
            disabled={!name || isSaving}
            sx={{
              background: 'linear-gradient(135deg, #8B5CF6 0%, #EC4899 100%)',
            }}
          >
            {isSaving ? 'Saving...' : 'Save'}
          </Button>
        </Box>
      </Box>
    </GlassCard>
  );
};
