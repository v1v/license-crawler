# List licenses of a particular GitHub Organisations


## Build docker image

    docker build -t license .

## Run docker image

    docker run --rm -ti -e GITHUB_TOKEN=XYZ -e GITHUB_USER=your -e GITHUB_ORG=jenkinsci license
