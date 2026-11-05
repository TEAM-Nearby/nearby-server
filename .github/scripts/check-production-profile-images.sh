#!/usr/bin/env bash
# 운영 EC2 역할로 프로필 이미지 저장과 CDN 조회가 가능한지 확인합니다.
set -euo pipefail

parameter_path="$1"
aws_region="$2"
read_setting() {
  local value
  value=$(aws ssm get-parameter --name "$parameter_path/$1" --region "$aws_region" \
    --query 'Parameter.Value' --output text)
  if [[ -z "$value" || "$value" == None ]]; then
    echo "Missing production image setting: $1" >&2
    return 1
  fi
  printf '%s\n' "$value"
}

bucket=$(read_setting NEARBY_STORAGE_S3_BUCKET)
region=$(read_setting NEARBY_STORAGE_S3_REGION)
cdn=$(read_setting NEARBY_STORAGE_S3_CDN_BASE_URL)
if [[ ! "$cdn" =~ ^https://[a-zA-Z0-9][a-zA-Z0-9.-]+/?$ ]]; then
  echo "Invalid production image CDN base URL" >&2
  exit 1
fi

probe_dir=$(mktemp -d)
trap 'rm -rf -- "$probe_dir"' EXIT
key="profiles/.deployment-check/$(cat /proc/sys/kernel/random/uuid).png"
printf '%s' 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAACQd1PeAAAADElEQVR4nGPQzXsIAAJIAX2VkPC1AAAAAElFTkSuQmCC' \
  | base64 --decode > "$probe_dir/upload.png"

aws s3api put-object --region "$region" --bucket "$bucket" --key "$key" \
  --content-type image/png --body "$probe_dir/upload.png" >/dev/null
curl -fsS --connect-timeout 5 --max-time 10 "${cdn%/}/$key" -o "$probe_dir/download.png"
cmp "$probe_dir/upload.png" "$probe_dir/download.png"
echo "Production profile image upload and download verified"
