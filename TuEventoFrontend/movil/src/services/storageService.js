const BASE_URL = process.env.EXPO_PUBLIC_API_URL;

export const getFileUrl = async (fileId, accessToken) => {
  const response = await fetch(`${BASE_URL}/storage/${fileId}`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  const json = await response.json();
  if (!json.success) throw new Error(json.message);
  const url = json.data.publicUrl.replace("localhost", process.env.EXPO_PUBLIC_MINIO_HOST ?? "localhost");
  return url;
};

export const uploadFile = async (imageUri, categoryCode, accessToken) => {
  const filename = imageUri.split("/").pop();
  const match = /\.(\w+)$/.exec(filename);
  const mimeType = match ? `image/${match[1].toLowerCase()}` : "image/jpeg";

  const formData = new FormData();
  formData.append("file", { uri: imageUri, name: filename, type: mimeType });
  formData.append("categoryCode", categoryCode);

  const response = await fetch(`${BASE_URL}/storage/upload`, {
    method: "POST",
    headers: { Authorization: `Bearer ${accessToken}` },
    body: formData,
  });
  const json = await response.json();
  if (!json.success) throw new Error(json.message);
  return json.data; // { storedFileId, s3Key, originalFilename, contentType, sizeBytes, publicUrl }
};
