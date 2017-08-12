import os
from setuptools import setup, find_packages

setup(name="quarty",
      version=os.environ.get("CIRCLE_BUILD_NUM", "unknown"),
      description="Quartic runner",
      author="Quartic Technologies",
      author_email="contact@quartic.io",
      url="https://www.quartic.io",
      license="MIT",
      packages=find_packages("src"),
      package_dir={"":"src"},
      install_requires=[
          # "requests==2.17.3",
          # "ipython==6.0.0",
          # "datadiff==2.0.0",
          # "pyarrow==0.4.0",
          # "pyproj==1.9.5.1",
          # "pandas==0.20.1",
          # "networkx==1.11"
      ],
      zip_safe=False)
