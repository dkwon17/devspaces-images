FROM quay.io/devspaces/machineexec-rhel9:3.18 as machine-exec

# https://registry.access.redhat.com/ubi9/ubi-micro
FROM registry.redhat.io/ubi9/ubi-micro:9.5-1733767087
COPY --from=machine-exec --chown=0:0 /go/bin/che-machine-exec /exec/machine-exec
