# REDHAWK Assets
 
This file contains information that pertains to all REDHAWK assets. For detailed information about an individual asset, see the README file in its sub-directory.

## Continuous Integration (CI)

This repository contains the REDHAWK assets. The CI build is performed by the `.gitlab-ci.yml` file in the root directory. This file needs to be generated when assets are added to the repository or a new version is created. 

To generate a new `.gitlab-ci.yml` file, run `generate_gitlab.py`. This script needs to be updated with release information (supported operating systems and version number). The file `gitlab-ci.yml.template` is used by `generate_gitlab.py` and contains information like the job and anchor descriptions.

## Dependencies

The assets in this repo depend on libraries `libRFSimulators` and `vrt`, which are in the `redhawk-dependencies` repo. The reason for this separation is because those two libraries install to the standard Linux locations rather than to `SDRROOT`.

## Branches and Tags

All REDHAWK assets use the same branching and tagging policy. Upon release, the `master` branch is rebased with the specific commit released, and that commit is tagged with the asset version number. For example, the commit released as version 1.0.0 is tagged with `1.0.0`.

Development branches (i.e. `develop` or `develop-X.X`, where *X.X* is a REDHAWK version number) contain the latest unreleased development code for the specified version. If no version number is specified (i.e. `develop`), the branch contains the latest unreleased development code for the latest released version.

## Copyright

This work is protected by Copyright. Please refer to the [COPYRIGHT](COPYRIGHT) file.

## License

Please refer to the [LICENSE](license/LICENSE) file for REDHAWK assets licensing information.
