    {{/* vim: set filetype=mustache: */}}
{{/*
Environment variables for web and worker containers
*/}}
{{- define "deployment.envs" }}
env:
  - name: SERVER_PORT
    value: "{{ .Values.image.port }}"

  - name: SPRING_PROFILES_ACTIVE
    value: "batch"

  - name: JAVA_OPTS
    value: ""

  - name: JWT_PUBLIC_KEY
    value: "{{ .Values.env.JWT_PUBLIC_KEY }}"

  - name: ELITE2_URI_ROOT
    value: "{{ .Values.env.ELITE2_URI_ROOT }}"

  - name: AUTH_URI_ROOT 
    value: "{{ .Values.env.AUTH_URI_ROOT }}"

  - name: SERVER_CONNECTION_TIMEOUT 
    value: "180000"

  - name: APPLICATION_INSIGHTS_IKEY 
    valueFrom:
      secretKeyRef:
        name: {{ template "app.name" . }}
        key: APPINSIGHTS_INSTRUMENTATIONKEY

  - name: ELITE2API_CLIENT_CLIENTID
    valueFrom:
      secretKeyRef:
        name: {{ template "app.name" . }}
        key: ELITE2API_CLIENT_CLIENTID

  - name: ELITE2API_CLIENT_CLIENTSECRET
    valueFrom:
      secretKeyRef:
        name: {{ template "app.name" . }}
        key: ELITE2API_CLIENT_CLIENTSECRET

  - name: SPRING_DATASOURCE_USERNAME
    valueFrom:
      secretKeyRef:
        name: dps-rds-instance-output
        key: database_username

  - name: SPRING_DATASOURCE_PASSWORD
    valueFrom:
      secretKeyRef:
        name: dps-rds-instance-output
        key: database_password

  - name: DB_NAME
    valueFrom:
      secretKeyRef:
        name: dps-rds-instance-output
        key: database_name

  - name: DB_ENDPOINT
    valueFrom:
      secretKeyRef:
        name: dps-rds-instance-output
        key: rds_instance_endpoint

  - name: APP_DB_URL
    value: "jdbc:postgresql://$(DB_ENDPOINT)/$(DB_NAME)?sslmode=verify-full"

{{- end -}}