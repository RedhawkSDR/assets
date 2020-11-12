#!/usr/bin/python

"""
Generate the `gitlab-ci.yml` file for REDHAWK assets.

`gitlab-ci.yml` needs to be generated when an asset is added or removed
from the repository, or for use with a new version of REDHAWK.

To generate `.gitlab-ci.yml`, run `generate_gitlab.py`. This script needs
to be updated with release information, including supported operating
systems and REDHAWK version number. The file `gitlab-ci.yml.template`
is used by `generate_gitlab.py` and contains information like the job
and anchor descriptions.
"""

import os, sys
from datetime import datetime
from getopt import getopt
import pprint

package_template = """package:__DIST____V__:rh__SHORT_V__:__ASSET_NAME__:
  stage: __BUILD__
  variables:
    dist: __DIST__
    uhd_repo: __UHDREPO__
    asset_name: __ASSET_NAME__
    lowercase_asset_name: __ASSET_LC_NAME__
__DEP__
  <<: *package

"""

create_template = """create-__DIST____V__:local:repos:
  stage: create_library_repo
  variables:
    dist: __DIST__
  dependencies:
__DEPS__
  <<: *create_local_repo

"""

create_libraries_template = """create-__DIST____V__:local:libraries:repos:
  stage: create_repo
  variables:
    dist: __DIST__
  dependencies:
__DEPS__
  <<: *create_local_repo

"""

create_comps_devs_template = """create-__DIST____V__:local:comps:devs:repos:
  stage: create_comps_devs_repo
  variables:
    dist: __DIST__
  dependencies:
__DEPS__
  <<: *create_local_repo

"""

test_template = """test:__DIST____V__:rh__SHORT_V__:__ASSET_NAME__:
  variables:
    dist: __DIST__
    uhd_repo: __UHDREPO__
    comp_type: __COMP_TYPE__
    asset_name: __ASSET_NAME__
    lowercase_asset_name: __ASSET_LC_NAME__
__DEP__
  <<: *test
"""

test_template_endpoint = """
    - test:__DIST__:rh__SHORT_V__:__ASSET_NAME__"""

test_template_add = """  only:
    - branches
"""

deploy_template = """deploy-__DIST____V__-__SHORT_V__:__ASSET_NAME__:
  variables:
    dist: __DIST__
    asset_name: __ASSET_NAME__
    lowercase_asset_name: __ASSET_LC_NAME__
  dependencies:
    - package:__DIST____V__:rh__SHORT_V__:__ASSET_NAME__
  <<: *s3

"""

create_repo_template = """create-repo:__DIST__:__SHORT_V__:
  variables:
    dist: __DIST__
  before_script:
    - echo "Building Final YUM repository rh__SHORT_V__  __DIST__"
  <<: *create-repo
  dependencies:
__DEPLOYJOBS__

"""


def replace_package_template(os_version, rh_version, comp_name, base_library=False, isComponentOrDevice=False, isWaveform=False):
    retval = package_template.replace('__DIST__', platforms[os_version]['dist']).replace('__LATEST_V__', versions[rh_version]['latest_version']).replace('__RELEASE_V__', versions[rh_version]['release_version']).replace('__SHORT_V__', versions[rh_version]['short_version']).replace('__ASSET_NAME__', comp_name).replace('__ASSET_LC_NAME__', comp_name.lower())

    if "el6" in os_version:
        retval = retval.replace('__UHDREPO__', '$s3_repo_url/redhawk-dependencies/uhd/yum/3.7.3/$dist/$arch')
    elif "el7" in os_version:
        retval = retval.replace('__UHDREPO__', '$s3_repo_url/redhawk-dependencies/uhd/yum/3.9.4/$dist/$arch')
    if base_library:
        retval = retval.replace('package', 'base_package')

    if base_library:
        retval = retval.replace("__DEP__\n", "")
    else:
        if isComponentOrDevice:
            retval = retval.replace("__DEP__\n", "  dependencies:\n    - create-"+platforms[os_version]['dist']+"__V__:local:libraries:repos\n")
        elif isWaveform:
            retval = retval.replace("__DEP__\n", "  dependencies:\n    - create-"+platforms[os_version]['dist']+"__V__:local:comps:devs:repos\n")
        else:
            retval = retval.replace("__DEP__\n", "  dependencies:\n    - create-"+platforms[os_version]['dist']+"__V__:local:repos\n")
    if isComponentOrDevice:
        retval = retval.replace("__BUILD__", "build_components")
    elif isWaveform:
        retval = retval.replace("__BUILD__", "build_waveforms")
    else:
        retval = retval.replace("__BUILD__", "build_libraries")

    if '32' in os_version:
        retval = retval.replace('__V__', ':32')
    else:
        retval = retval.replace('__V__', '')
    return retval

def replace_test_job_name_template(os_version, rh_version, comp_name, branches=False, base_library=False, isComponent=True):
    retval = test_template_endpoint.replace('__DIST__', platforms[os_version]['dist']).replace('__SHORT_V__', versions[rh_version]['short_version']).replace('__ASSET_NAME__', comp_name)
    return retval

def replace_test_template(os_version, rh_version, comp_name, branches=False, base_library=False, isComponent=True):
    retval = test_template.replace('__DIST__', platforms[os_version]['dist']).replace('__ARCH__', platforms[os_version]['arch']).replace('__LATEST_V__', versions[rh_version]['latest_version']).replace('__RELEASE_V__', versions[rh_version]['release_version']).replace('__SHORT_V__', versions[rh_version]['short_version']).replace('__ASSET_NAME__', comp_name).replace('__ASSET_LC_NAME__', comp_name.lower())

    if "el6" in os_version:
        retval = retval.replace('__UHDREPO__', '$s3_repo_url/redhawk-dependencies/uhd/yum/3.7.3/$dist/$arch')
    elif "el7" in os_version:
        retval = retval.replace('__UHDREPO__', '$s3_repo_url/redhawk-dependencies/uhd/yum/3.9.4/$dist/$arch')
    if isComponent:
        retval = retval.replace('__COMP_TYPE__', 'components')
    else:
        retval = retval.replace('__COMP_TYPE__', 'devices')
    if branches:
        retval += test_template_add
    retval += '\n'
    if base_library:
        retval = retval.replace("__DEP__\n", "")
    else:
        retval = retval.replace("__DEP__\n", "  dependencies:\n    - create-"+platforms[os_version]['dist']+"__V__:local:libraries:repos\n")
    if '32' in os_version:
        retval = retval.replace('__V__', ':32')
    else:
        retval = retval.replace('__V__', '')
    return retval

def replace_deploy_template(os_version, rh_version, comp_name, base_library=False):
    retval = deploy_template.replace('__DIST__', platforms[os_version]['dist']).replace('__ARCH__', platforms[os_version]['arch']).replace('__ASSET_NAME__', comp_name).replace('__ASSET_LC_NAME__', comp_name.lower()).replace('__SHORT_V__', versions[rh_version]['short_version'])

    if base_library:
        retval = retval.replace('package', 'base_package')
    if '32' in os_version:
        retval = retval.replace('__V__', ':32')
    else:
        retval = retval.replace('__V__', '')
    return retval


def replace_create_repo_template(os_version, rh_version, deploy_jobs):
    retval = create_repo_template.replace('__DIST__', platforms[os_version]['dist']).replace('__ARCH__', platforms[os_version]['arch']).replace('__LATEST_V__', versions[rh_version]['latest_version']).replace('__RELEASE_V__', versions[rh_version]['release_version']).replace('__SHORT_V__', versions[rh_version]['short_version'])
    
    deploy_list = ''
    for dep in deploy_jobs:
        deploy_list += '    - '+dep+'\n'

    retval = retval.replace("__DEPLOYJOBS__\n", deploy_list)
    if '32' in os_version:
        retval = retval.replace('__V__', ':32')
    else:
        retval = retval.replace('__V__', '')
    return retval



def replace_create_template(os_version, rh_version, objects):
    comp_dep_list = ''
    for comp in objects:
        if comp == 'dsp':
            comp_dep_list += '    - base_package:'+platforms[os_version]['dist']+'__V__:rh'+rh_version+':'+comp+'\n'

    retval = create_template.replace('__DEPS__', comp_dep_list).replace('__DIST__', platforms[os_version]['dist']).replace('__SHORT_V__', rh_version).replace('__ARCH__', platforms[os_version]['arch'])
    if '32' in os_version:
        retval = retval.replace('__V__', ':32')
    else:
        retval = retval.replace('__V__', '')
    return retval

def replace_create_libraries_template(os_version, rh_version, objects):
    comp_dep_list = ''
    for comp in objects:
        if comp == 'dsp':
            comp_dep_list += '    - base_package:'+platforms[os_version]['dist']+'__V__:rh'+rh_version+':'+comp+'\n'
        else:
            comp_dep_list += '    - package:'+platforms[os_version]['dist']+'__V__:rh'+rh_version+':'+comp+'\n'

    retval = create_libraries_template.replace('__DEPS__', comp_dep_list).replace('__DIST__', platforms[os_version]['dist']).replace('__SHORT_V__', rh_version).replace('__ARCH__', platforms[os_version]['arch'])
    if '32' in os_version:
        retval = retval.replace('__V__', ':32')
    else:
        retval = retval.replace('__V__', '')
    return retval

def replace_create_comps_devs_template(os_version, rh_version, objects):
    comp_dep_list = ''
    for comp in objects:
        if comp == 'dsp':
            comp_dep_list += '    - base_package:'+platforms[os_version]['dist']+'__V__:rh'+rh_version+':'+comp+'\n'
        else:
            comp_dep_list += '    - package:'+platforms[os_version]['dist']+'__V__:rh'+rh_version+':'+comp+'\n'

    retval = create_comps_devs_template.replace('__DEPS__', comp_dep_list).replace('__DIST__', platforms[os_version]['dist']).replace('__SHORT_V__', rh_version).replace('__ARCH__', platforms[os_version]['arch'])
    if '32' in os_version:
        retval = retval.replace('__V__', ':32')
    else:
        retval = retval.replace('__V__', '')
    return retval

usage = """%s [options]

Options:
    --coreversion <name>         Name for RH core framework branch to build assets against [default: None, same as the asset version]
    --testonly                   Generate a gitlab pipeline for test only [default: False]
    --help                       Print this message
""" % (os.path.basename(sys.argv[0]))

if __name__ == '__main__':

    longopts = ['help', 'coreversion=', 'testonly']

    coreversion = ''
    testonly = False
    opts, args = getopt(sys.argv[1:], 'h', longopts)
    for key, value in opts:
        if key == '--help':
            raise SystemExit(usage)
        elif key == '--coreversion':
            coreversion = value
        elif key == '--testonly':
            testonly = True

    fp=open('gitlab-ci.yml.template','r')
    contents=fp.read()
    fp.close()

    platforms = {}
    versions = {}

    platforms['el7']={'dist':'el7', 'arch':'x86_64'}
    #versions['2.0']={'latest_version':'develop-2-0', 'release_version':'$rh_20_release', 'short_version':'2.0', 'platform_keys':['el6', 'el6:32', 'el7']}
    versions['2.2']={'latest_version':'develop-2-2', 'release_version':'$rh_22_release', 'short_version':'2.2', 'platform_keys':['el7']}

    base_component_dir = 'sdr/libraries'
    candidate_components = os.listdir(base_component_dir)
    libraries = []
    for comp in candidate_components:
        if os.path.isfile(base_component_dir+'/'+comp+'/'+comp+'.spd.xml'):
            libraries.append(comp)

    base_component_dir = 'sdr/components'
    candidate_components = os.listdir(base_component_dir)
    components = []
    for comp in candidate_components:
        if os.path.isfile(base_component_dir+'/'+comp+'/'+comp+'.spd.xml'):
            components.append(comp)
    base_component_dir = 'sdr/devices'
    candidate_components = os.listdir(base_component_dir)
    devices = []
    for comp in candidate_components:
        if os.path.isfile(base_component_dir+'/'+comp+'/'+comp+'.spd.xml'):
            components.append(comp)
            devices.append(comp)

    base_component_dir = 'sdr/waveforms'
    candidate_components = os.listdir(base_component_dir)
    waveforms = []
    for comp in candidate_components:
        if os.path.isfile(base_component_dir+'/'+comp+'/'+comp+'.sad.xml'):
            waveforms.append(comp)

    jobs = ''
    test_jobs = ''
    deploy_jobs= {}

    for _key in versions[next(iter(versions))]['platform_keys']:
        jobs += replace_create_template(_key, versions[next(iter(versions))]['short_version'], libraries)

    for comp in libraries:
        base_package = False
        if comp == 'dsp':
            base_package = True
        rh_version = next(iter(versions))
        for os_version in versions[next(iter(versions))]['platform_keys']:
            jobs += replace_package_template(os_version, rh_version, comp, base_package)
        if not testonly:
            for os_version in versions[next(iter(versions))]['platform_keys']:
                deploy_job = replace_deploy_template(os_version, rh_version, comp, base_package)
                deploy_jobs.setdefault(os_version,[]).append(deploy_job.split(':\n')[0])
                jobs +=  deploy_job

    for _key in versions[next(iter(versions))]['platform_keys']:
        jobs += replace_create_libraries_template(_key, versions[next(iter(versions))]['short_version'], libraries)

    for comp in components:
        base_package = False
        isComponentOrDevice = True
        isComponent = True
        if comp in devices:
            isComponent = False

        rh_version = next(iter(versions))
        if not testonly:
            for os_version in versions[next(iter(versions))]['platform_keys']:
                jobs += replace_package_template(os_version, rh_version, comp, base_package, isComponentOrDevice)

        if (comp != 'MSDD'):
            for os_version in versions[next(iter(versions))]['platform_keys']:
                test_jobs += replace_test_job_name_template(os_version, rh_version, comp, False, base_package, isComponent)
                jobs += replace_test_template(os_version, rh_version, comp, False, base_package, isComponent)

        if not testonly:
            for os_version in versions[next(iter(versions))]['platform_keys']:
                deploy_job = replace_deploy_template(os_version, rh_version, comp, base_package)
                deploy_jobs.setdefault(os_version,[]).append(deploy_job.split(':\n')[0])                
                jobs +=  deploy_job

    if not testonly:
        comps_devs = libraries + components

        for _key in versions[next(iter(versions))]['platform_keys']:
            jobs += replace_create_comps_devs_template(_key, versions[next(iter(versions))]['short_version'], comps_devs)

        for comp in waveforms:
            base_package = False
            isComponent = False
            isWaveform = True

            rh_version = next(iter(versions))
            for os_version in versions[next(iter(versions))]['platform_keys']:
                jobs += replace_package_template(os_version, rh_version, comp, base_package, isComponent, isWaveform)

            for os_version in versions[next(iter(versions))]['platform_keys']:
                deploy_job = replace_deploy_template(os_version, rh_version, comp, base_package)
                deploy_jobs.setdefault(os_version,[]).append(deploy_job.split(':\n')[0])                
                jobs += deploy_job

    for os_version in versions[next(iter(versions))]['platform_keys']:
        if os_version in deploy_jobs:
            jobs += replace_create_repo_template(os_version, rh_version, deploy_jobs[os_version])               


    updated_contents = contents.replace('__JOBS__', jobs)
    updated_contents = updated_contents.replace('__TESTJOBS__', test_jobs)
    if coreversion:
        updated_contents = updated_contents.replace('__REDHAWK_VERSION__', coreversion)
    else:
        updated_contents = updated_contents.replace('__REDHAWK_VERSION__', '$redhawk_version')
    if testonly:
        updated_contents = updated_contents.replace('__BUILDCOMPONENTS__', '')
    else:
        updated_contents = updated_contents.replace('__BUILDCOMPONENTS__', '\n  - build_components\n  - create_comps_devs_repo\n  - build_waveforms')

    updated_contents = updated_contents.replace('__LATEST_V__', versions[rh_version]['latest_version']).replace('__RELEASE_V__', versions[rh_version]['release_version']).replace('__SHORT_V__', versions[rh_version]['short_version']).replace('__ARCH__', platforms[os_version]['arch'])

    do_not_modify = ('# AUTO-GENERATED CODE. DO NOT MODIFY.\n'
                     '# generated by:  {0}\n'
                     '# generated on:  {1}\n\n'
                     ''.format(os.path.basename(__file__), datetime.utcnow().date()))

    fp=open('.gitlab-ci.yml','w')
    fp.write(do_not_modify)
    fp.write(updated_contents)
    fp.close()
